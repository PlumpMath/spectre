package skadistats.spectre.persist.fs;

import java.io.*;
import java.util.List;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import skadistats.spectre.err.*;
import skadistats.spectre.proto.internal.Fs.StringTable;


public class FSReader extends skadistats.spectre.persist.stream.StreamReader {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private String filePath;

    private final static int MSG_TICK     = 0;
    private final static int MSG_STRINGS  = 1000;
    private final static int MAX_MSG_SIZE = 4096;
    private final static int MAX_STABLE_SIZE = 32*1024*1024;

    public FSReader(String aspectBase, String aspectPath, int replayId) 
        throws AspectNotFound, MalformedAspect {

        filePath = aspectBase+"/"+aspectPath+"/"+replayId;
        try {
            setStream(new DataInputStream(new BufferedInputStream(
                                          new GzipCompressorInputStream(
                                          new FileInputStream(filePath)))));
        } catch(FileNotFoundException fnf) {
            throw new AspectNotFound(fnf.toString());
        } catch(IOException ioe) {
            throw new MalformedAspect(ioe.toString());
        }
    }

    public List<String> readStringTable() throws IOException {
        /* Open a new copy of the file, seek to the end, and parse StringTable */
        RandomAccessFile raf = new RandomAccessFile(filePath, "r");
        raf.seek(raf.length()-4); // strtable offset data
        log.trace("File length:{}, Offset pos:{}", raf.length(), raf.length()-4);
        int strOff = raf.readInt();
        log.trace("Offset value:{}", strOff);
        if (strOff > raf.length())
            throw new MalformedAspect("StringTable corrupted: strOff too big:"+strOff); 
        raf.seek(raf.length()-strOff); // seek to beginning of strtable entry
        int strSize = strOff-4;

        // Read gzip'd string table data in to memory
        if (strSize > MAX_STABLE_SIZE)
            throw new MalformedAspect("StringTable size:"+strSize+" > "+MAX_STABLE_SIZE);
        byte[] msgData = new byte[strSize];
        log.debug("Reading StringTable from {}, size:{}", raf.getFilePointer(), strSize);
        raf.readFully(msgData);

        // Read protobuf data from gzip'd stream
        DataInputStream strTableIn = new DataInputStream(new GzipCompressorInputStream(
                                                         new ByteArrayInputStream(msgData)));

        StringTable strTable = StringTable.parseFrom(strTableIn);
        List<String> strData = strTable.getValueList();
        raf.close();

        log.trace("strTable.length:{}", strData.size());
        int i;
        for (i=0; i<strData.size(); ++i) {
            log.trace("\t{}: {}", i, strData.get(i));
        }

        return strData;
    }

}
