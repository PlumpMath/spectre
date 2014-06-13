package skadistats.spectre.persist.s3;

import java.io.*;
import java.util.List;
import org.apache.mahout.math.Varint;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.*;
import javax.xml.bind.DatatypeConverter;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;

import skadistats.spectre.err.*;
import skadistats.spectre.persist.AspectSerializer;
import skadistats.spectre.proto.internal.Fs.StringTable;

public class S3Writer extends skadistats.spectre.persist.stream.StreamWriter {
    private final Logger log = LoggerFactory.getLogger(getClass());

    protected AmazonS3 s3Client;
    protected String   s3Bucket;
    protected String   s3Key;
    protected S3Object s3Obj;

    protected ByteArrayOutputStream memBuf;;

    private final static int MAX_BUFFER_SIZE = 64*1024*1024; // 64MB

    public S3Writer(String s3Bucket, String aspectPath, int replayId) {

        if (aspectPath.startsWith("/")) {
            aspectPath = aspectPath.substring(1);
        }

        this.s3Client = new AmazonS3Client(new DefaultAWSCredentialsProviderChain());
        this.s3Bucket = s3Bucket;
        this.s3Key    = aspectPath+"/"+replayId;

        try {

            // Write aspect to in-memory buffer because need object size before upload
            memBuf = new ByteArrayOutputStream();
            setStream(new DataOutputStream(new BufferedOutputStream(
                                           new GzipCompressorOutputStream(memBuf))),
                      replayId);
        } catch(IOException ioe) {
            throw new MalformedAspect(ioe.toString());
        }
    }

    public void writeStringTable(List<String> strTable) throws IOException {

        // Write EOF marker
        log.trace("Writing MSG_STRINGS at {}", memBuf.size());
        Varint.writeUnsignedVarInt(MSG_STRINGS, dataOut);    // msgType
        dataOut.close();
        log.trace("aspect size:{}", memBuf.size());

        int preStrTableSz = memBuf.size();

        // append StringTable to the end of the file
        dataOut = new DataOutputStream(new GzipCompressorOutputStream(memBuf));

        byte[] msgData = StringTable.newBuilder()
                         .addAllValue(strTable)
                         .build()
                         .toByteArray(); 
        log.trace("Writing StringTable at {}", memBuf.size());
        dataOut.write(msgData);                              // msgData
        dataOut.close();
        int strTableSz = memBuf.size()-preStrTableSz;
        log.trace("StringTable size:{}", strTableSz);
        log.trace("aspect size:{}", memBuf.size());

        // write (offset from EOF to beginning of StringTable) to the end of file
        dataOut = new DataOutputStream(memBuf);

        log.trace("Writing Offset at {}, value:{}", memBuf.size(), strTableSz+4);
        dataOut.writeInt(strTableSz+4);
        
        // this must be the last thing written to the stream
        dataOut.close();
        log.trace("aspect size:{}", memBuf.size());

        // upload aspect
        s3Put();
    }

    private void s3Put() {
        byte[] objData = memBuf.toByteArray();

        // calculate object checksum
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae) { 
            throw new RuntimeException(nsae.toString());
        }
        String chksum = DatatypeConverter.printBase64Binary(md.digest(objData));

        // prepare object metadata
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(objData.length);
        meta.setContentMD5(chksum);

        // upload object
        log.trace("putObject:{}/{} | size:{}", s3Bucket, s3Key, objData.length);
        s3Client.putObject(s3Bucket, s3Key, new ByteArrayInputStream(objData), meta);
    }

}
