package skadistats.spectre.persist.stream;

import java.io.*;
import java.util.List;
import org.apache.mahout.math.Varint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import skadistats.spectre.err.*;
import skadistats.spectre.persist.AspectSerializer;
import skadistats.spectre.persist.Helpers.MessageEnvelope;


public abstract class StreamWriter implements AspectSerializer {
    private final Logger log = LoggerFactory.getLogger(getClass());

    protected int currentTick;
    protected DataOutputStream dataOut;

    protected final static int MSG_TICK     = 0;
    protected final static int MSG_STRINGS  = 1000;

    public void setStream(DataOutputStream dataOut, int replayId) 
        throws IOException {

        this.dataOut = dataOut;
        
        // write replayId
        Varint.writeUnsignedVarInt(replayId, dataOut);
        log.debug("replayId:{}", replayId);
    }

    public void setReplayId(int replayId) throws IOException {
        throw new UnsupportedOperationException();
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

    private void writeTick(int tick) throws IOException {
        // write msg type
        Varint.writeUnsignedVarInt(MSG_TICK, dataOut);
        // write msg
        Varint.writeUnsignedVarInt(tick, dataOut);
        log.trace("writeTick: tick:{}", tick);
    }

}
