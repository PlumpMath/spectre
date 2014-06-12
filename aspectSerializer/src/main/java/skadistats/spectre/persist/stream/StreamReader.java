package skadistats.spectre.persist.stream;

import java.io.*;
import org.apache.mahout.math.Varint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import skadistats.spectre.err.*;
import skadistats.spectre.persist.AspectDeserializer;
import skadistats.spectre.persist.Helpers.MessageEnvelope;
import skadistats.spectre.proto.internal.Fs.StringTable;


public abstract class StreamReader implements AspectDeserializer {
    private final Logger log = LoggerFactory.getLogger(getClass());

    protected int replayId;
    protected int currentTick;
    protected DataInputStream dataIn;

    private final static int MSG_TICK     = 0;
    private final static int MSG_STRINGS  = 1000;
    private final static int MAX_MSG_SIZE = 4096;

    public void setStream(DataInputStream dataIn) throws MalformedAspect {
        // validate we have mark/reset support or quit now
        if (!dataIn.markSupported()) {
            throw new RuntimeException("mark/reset not supported for dataIn stream");
        }

        this.dataIn = dataIn;
        
        // read replay id
        try {
            this.replayId = Varint.readUnsignedVarInt(dataIn);
        } catch (IOException ioe) {
            throw new MalformedAspect(ioe.toString());
        }
    }

    public int getReplayId() throws IOException { return replayId; }

    public boolean hasNext() {
        try {
            while (true) {
                dataIn.mark(32);
                int msgType = Varint.readUnsignedVarInt(dataIn);

                switch (msgType) {
                    case MSG_TICK:
                        currentTick = Varint.readUnsignedVarInt(dataIn);
                        break;
                    case MSG_STRINGS:
                        return false; // at end of file
                    default:
                        dataIn.reset();
                        return true; 
                }

            }
        } catch(IOException ex) {
            log.warn("IOException: {}", ex.toString());
            return false;
        }
    }

    public MessageEnvelope readMsg() throws IOException {
        int msgType = Varint.readUnsignedVarInt(dataIn);
        if (msgType == MSG_TICK || msgType == MSG_STRINGS)
            throw new RuntimeException("readMsg called for INTERNAL MsgType:"+msgType); 

        int msgSize = Varint.readUnsignedVarInt(dataIn);

        if (msgSize <= MAX_MSG_SIZE) {
            byte[] byteBuf = new byte[msgSize];
            dataIn.readFully(byteBuf);
            return new MessageEnvelope(currentTick, msgType, byteBuf);
        }
        else {
            throw new MalformedAspect("MAX_MSG_SIZE exceeded, MsgSize:"+msgSize);
        }
    }
}
