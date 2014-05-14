package skadistats.spectre.persist.fs;

import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import skadistats.spectre.err.*;


public class FSWriter extends OutputStream {
    protected OutputStream fileOut;

    public FSWriter(String aspectBase, String aspectPath, int replayId) 
        throws AspectNotFound {

        File filePath = new File(aspectBase+"/"+aspectPath+"/"+replayId);
        try {
            if (!filePath.getParentFile().exists()) 
                filePath.getParentFile().mkdirs();

            fileOut = new FileOutputStream(filePath);
        } catch (FileNotFoundException ex) {
            throw new AspectNotFound(ex.toString());
        }
    }

    public void close() throws IOException { fileOut.close(); }
    public void flush() throws IOException { fileOut.flush(); }

    public void write(int b) throws IOException { fileOut.write(b); }
}
