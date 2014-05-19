package skadistats.spectre.persist.fs;

import java.io.*;
import java.util.List;
import org.apache.mahout.math.Varint;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import skadistats.spectre.err.*;
import skadistats.spectre.proto.internal.Fs.StringTable;
import skadistats.spectre.persist.AspectSerializer;
import skadistats.spectre.persist.Helpers.MessageEnvelope;


public class FSWriter implements AspectSerializer {
    private final Logger log = LoggerFactory.getLogger(getClass());

    protected int currentTick;
    protected DataOutputStream dataOut;

    private final static int MSG_TICK     = 0;
    private final static int MSG_STRINGS  = 1000;

    public FSWriter(String aspectBase, String aspectPath, int replayId) 
        throws AspectNotFound, IOException {

        File filePath = new File(aspectBase+"/"+aspectPath+"/"+replayId);
        try {
            if (!filePath.getParentFile().exists()) 
                filePath.getParentFile().mkdirs();

            dataOut = new DataOutputStream(new GzipCompressorOutputStream(
                                           new FileOutputStream(filePath)));
        } catch (FileNotFoundException ex) {
            throw new AspectNotFound(ex.toString());
        }
        
        // write replayId
        Varint.writeUnsignedVarInt(replayId, dataOut);
        log.debug("replayId:{}", replayId);
    }

    public void setReplayId(int replayId) throws IOException {
        throw new RuntimeException("setReplayId not supported in this implementation");
    }

    public void writeMsg(MessageEnvelope msgEnv) throws IOException {
        if (msgEnv.tick > currentTick) {
            writeTick(msgEnv.tick);
            currentTick = msgEnv.tick;
        } else if (msgEnv.tick < currentTick) {
            throw new InvalidTick("tick:"+msgEnv.tick+" < currentTick:"+currentTick);
        }
        
        Varint.writeUnsignedVarInt(msgEnv.msgType, dataOut);        // msgType
        Varint.writeUnsignedVarInt(msgEnv.msgData.length, dataOut); // msgSize
        dataOut.write(msgEnv.msgData);                              // msgData

        log.trace("writeMsg: msgType:{}, length:{}", msgEnv.msgType, msgEnv.msgData.length);
    }
    
    public void close(List<String> strTable) throws IOException {
        // append StringTable to the end of file
        byte[] msgData = StringTable.newBuilder()
                         .addAllValue(strTable)
                         .build()
                         .toByteArray();

        Varint.writeUnsignedVarInt(MSG_STRINGS, dataOut);    // msgType
        log.trace("Writing StringTable at {}, size:{}", dataOut.size(), msgData.length);
        dataOut.write(msgData);                              // msgData

        // write offset from EOF to beginning of StringTable
        log.trace("Writing Offset at {}, value:{}", dataOut.size(), msgData.length+4);
        dataOut.writeInt(msgData.length+4);
        
        // this must be the last thing written to the stream
        dataOut.close(); 
    }

    private void writeTick(int tick) throws IOException {
        // write msg type
        Varint.writeUnsignedVarInt(MSG_TICK, dataOut);
        // write msg
        Varint.writeUnsignedVarInt(tick, dataOut);
        log.trace("writeTick: tick:{}", tick);
    }

}
