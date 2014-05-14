package skadistats.spectre.persist.fs;

import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import skadistats.spectre.err.*;


public class FSWriter extends OutputStream {
    protected OutputStream fileOut;

    public FSWriter(String aspectBase, String aspectPath, int replayId) 
        throws AspectNotFound {

        String filePath = aspectBase+"/"+aspectPath+"/"+replayId;
        try {
            fileOut = new FileOutputStream(filePath);
        } catch (FileNotFoundException ex) {
            throw new AspectNotFound(ex.toString());
        }
    }

    public void close() throws IOException { fileOut.close(); }
    public void flush() throws IOException { fileOut.flush(); }

    public void write(int b) throws IOException { fileOut.write(b); }
}
