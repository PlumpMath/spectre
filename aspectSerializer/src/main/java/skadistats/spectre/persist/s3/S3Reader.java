package skadistats.spectre.persist.s3;

import java.io.*;
import java.util.List;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import skadistats.spectre.err.*;
import skadistats.spectre.proto.internal.Fs.StringTable;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;


public class S3Reader extends skadistats.spectre.persist.stream.StreamReader {
    private final Logger log = LoggerFactory.getLogger(getClass());

    protected AmazonS3 s3Client;
    protected String   s3Bucket;
    protected String   s3Key;
    protected S3Object s3Obj;

    private final static int MSG_TICK     = 0;
    private final static int MSG_STRINGS  = 1000;
    private final static int MAX_MSG_SIZE = 4096;
    private final static int MAX_STABLE_SIZE = 32*1024*1024;

    public S3Reader(String s3Bucket, String aspectPath, int replayId) 
        throws AspectNotFound, MalformedAspect {

        if (aspectPath.startsWith("/")) {
            aspectPath = aspectPath.substring(1);
        }

        this.s3Client = new AmazonS3Client(new DefaultAWSCredentialsProviderChain());
        this.s3Bucket = s3Bucket;
        this.s3Key    = aspectPath+"/"+replayId;

        try {
            log.trace("getObject:{}/{}", s3Bucket, s3Key);
            s3Obj = s3Client.getObject(s3Bucket, s3Key);

            setStream(new DataInputStream(new BufferedInputStream(
                                          new GzipCompressorInputStream(
                                          s3Obj.getObjectContent()))));
        } catch(AmazonServiceException ase) {
            throw new MalformedAspect(ase.toString());
        } catch(AmazonClientException ace) {
            throw new AspectNotFound(ace.toString());
        } catch(IOException ioe) {
            throw new MalformedAspect(ioe.toString());
        }

    }

    public List<String> readStringTable() throws IOException {
        /* Pay particularly close attention the order in which the
           buffering and creation of the new gzip stream occures.

           Proper order:
           1. Allocate byte[] = offset value - 4 
              (4 bytes for the offset value at the end of the file)

           2. Fill this buffer from the raw (un-gzip'd) stream

           3. Then wrap the byte[] in a gzip reader

           Note:
              If you create a new gzip'd stream and then try to
              fill the buffer it won't work because the number of
              gzip'd bytes != number of raw bytes in stream.
        */

        ////////////////////////////////////////////
        // get aspect size
        log.trace("getObjectMetadata:{}/{}", s3Bucket, s3Key);
        long aspectSz = s3Client.getObjectMetadata(s3Bucket, s3Key).getContentLength();
        log.trace("File length:{}, Offset pos:{}", aspectSz, aspectSz-4);

        ////////////////////////////////////////////
        // download last 4 bytes (string table offset)
        S3Object offsetObj = s3Client.getObject(new GetObjectRequest(s3Bucket,s3Key)
                                                .withRange(aspectSz-4, aspectSz-1));
        DataInputStream offsetIS = new DataInputStream(offsetObj.getObjectContent());
        int offsetVal = offsetIS.readInt();
        offsetObj.close();
        log.trace("Offset value:{}", offsetVal);
        if (offsetVal > aspectSz || offsetVal < 0) {
            throw new MalformedAspect("StringTable corrupted: offset invalid:"+offsetVal); 
        }

        ////////////////////////////////////////////
        // download gzip'd string table into memory
        int stSz = new Long(offsetVal-4).intValue();
        long start = aspectSz-offsetVal;
        long end   = start+stSz-1;
        S3Object stObj = s3Client.getObject(new GetObjectRequest(s3Bucket,s3Key)
                                            .withRange(start, end));
        if (stSz > MAX_STABLE_SIZE)
            throw new MalformedAspect("StringTable size:"+stSz+" > "+MAX_STABLE_SIZE);
        byte[] msgData = new byte[stSz];
        log.debug("Reading StringTable data {}-{}", start, end);
        InputStream is = stObj.getObjectContent();
        int nRead;
        int nOff = 0;
        while ((nRead = is.read(msgData, nOff, stSz-nOff)) != -1 || stSz-nOff == 0) {
            nOff += nRead;
        }
        stObj.close();

        ////////////////////////////////////////////
        // Read protobuf data from gzip'd stream
        DataInputStream strTableIn = new DataInputStream(new GzipCompressorInputStream(
                                                         new ByteArrayInputStream(msgData)));

        StringTable strTable = StringTable.parseFrom(strTableIn);
        List<String> strData = strTable.getValueList();

        log.trace("strTable.length:{}", strData.size());
        int i;
        for (i=0; i<strData.size(); ++i) {
            log.trace("\t{}: {}", i, strData.get(i));
        }

        return strData;
    }

}
