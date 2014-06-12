package skadistats.spectre.persist.fs;

import java.io.*;
import java.util.List;
import org.apache.mahout.math.Varint;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import skadistats.spectre.err.*;
import skadistats.spectre.proto.internal.Fs.StringTable;


public class FSWriter extends skadistats.spectre.persist.stream.StreamWriter {
    private final Logger log = LoggerFactory.getLogger(getClass());

    protected File filePath;

    public FSWriter(String aspectBase, String aspectPath, int replayId) 
        throws AspectNotFound, IOException {

        filePath = new File(aspectBase+"/"+aspectPath+"/"+replayId);
        try {
            if (!filePath.getParentFile().exists()) 
                filePath.getParentFile().mkdirs();

            setStream(new DataOutputStream(new GzipCompressorOutputStream(
                                           new BufferedOutputStream(
                                           new FileOutputStream(filePath)))),
                  replayId);
        } catch (FileNotFoundException ex) {
            throw new AspectNotFound(ex.toString());
        }
    }

    public void writeStringTable(List<String> strTable) throws IOException {

        // flush data to file
        log.trace("Writing MSG_STRINGS at {}", filePath.length()+dataOut.size());
        Varint.writeUnsignedVarInt(MSG_STRINGS, dataOut);    // msgType
        dataOut.close();
        log.trace("aspect size:{}", filePath.length());
        long preStrTableSz = filePath.length();

        // append StringTable to the end of the file
        try {
            if (!filePath.getParentFile().exists()) 
                filePath.getParentFile().mkdirs();

            dataOut = new DataOutputStream(new GzipCompressorOutputStream(
                                           new BufferedOutputStream(
                                           new FileOutputStream(filePath,true))));
        } catch (FileNotFoundException ex) {
            throw new IOException(ex.toString());
        }

        byte[] msgData = StringTable.newBuilder()
                         .addAllValue(strTable)
                         .build()
                         .toByteArray(); 
        log.trace("Writing StringTable at {}", filePath.length()+dataOut.size());
        dataOut.write(msgData);                              // msgData
        dataOut.close();
        Long strTableSz = new Long(filePath.length()-preStrTableSz);
        log.trace("StringTable size:{}", strTableSz);
        log.trace("aspect size:{}", filePath.length());

        // write offset from EOF to beginning of StringTable to the end of file
        try {
            if (!filePath.getParentFile().exists()) 
                filePath.getParentFile().mkdirs();

            dataOut = new DataOutputStream(new BufferedOutputStream(
                                           new FileOutputStream(filePath,true)));
        } catch (FileNotFoundException ex) {
            throw new IOException(ex.toString());
        }

        log.trace("Writing Offset at {}, value:{}", filePath.length()+dataOut.size(), strTableSz+4);
        dataOut.writeInt(strTableSz.intValue()+4);
        
        // this must be the last thing written to the stream
        dataOut.close();
        log.trace("aspect size:{}", filePath.length());
    }

}
