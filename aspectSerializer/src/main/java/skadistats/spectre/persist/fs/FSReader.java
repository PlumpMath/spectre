package skadistats.spectre.persist.fs;

import java.io.*;
import java.util.List;
import org.apache.mahout.math.Varint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import skadistats.spectre.err.*;
import skadistats.spectre.persist.AspectDeserializer;
import skadistats.spectre.persist.Helpers.MessageEnvelope;
import skadistats.spectre.proto.internal.Fs.StringTable;


public class FSReader implements AspectDeserializer {
    private final Logger log = LoggerFactory.getLogger(getClass());

    protected int replayId;
    protected int currentTick;
    protected InputStream fileIn;
    private DataInputStream dataIn;
    private String filePath;

    private final static int MSG_TICK     = 0;
    private final static int MSG_STRINGS  = 1000;
    private final static int MAX_MSG_SIZE = 4096;

    public FSReader(String aspectBase, String aspectPath, int replayId) 
        throws AspectNotFound, MalformedAspect {

        // open file
        filePath = aspectBase+"/"+aspectPath+"/"+replayId;
        try {
            fileIn = new BufferedInputStream(new FileInputStream(filePath));
            dataIn = new DataInputStream(fileIn);
        } catch(FileNotFoundException ex) {
            throw new AspectNotFound(ex.toString());
        }
        
        // read replay id
        try {
            this.replayId = Varint.readUnsignedVarInt(dataIn);
        } catch (IOException ioe) {
            throw new MalformedAspect(ioe.toString());
        }
    }

    public int getReplayId() throws IOException { return replayId; }

    public List<String> getStringTable() throws IOException {
        /* Open a new copy of the file, seek to the end, and parse StringTable */
        RandomAccessFile raf = new RandomAccessFile(filePath, "r");
        raf.seek(raf.length()-4); // strtable offset data
        int strOff = raf.readInt();
        if (strOff > raf.length())
            throw new MalformedAspect("StringTable corrupted: strOff too big:{}", strOff); 
        raf.seek(raf.length()-strOff); // seek to beginning of strtable entry
        int strSize = strOff-4;

        if (strSize > 4096)
            throw new MalformedAspect("StringTable size:"+strSize+" > 4096");
        byte[] msgData = new byte[strSize];
        log.debug("Reading StringTable from {}, size:{}", raf.getFilePointer(), strSize);
        raf.readFully(msgData);
        StringTable strTable = StringTable.parseFrom(msgData);
        raf.close();

        return strTable.getValueList();
    }

    public boolean hasNext() {
        try {
            while (true) {
                fileIn.mark(32);
                int msgType = Varint.readUnsignedVarInt(dataIn);

                switch (msgType) {
                    case MSG_TICK:
                        currentTick = Varint.readUnsignedVarInt(dataIn);
                        break;
                    case MSG_STRINGS:
                        return false; // at end of file
                    default:
                        fileIn.reset();
                        return true; 
                }

            }
        } catch(IOException ex) {
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
