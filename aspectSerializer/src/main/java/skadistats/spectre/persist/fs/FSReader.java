package skadistats.spectre.persist.fs;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import skadistats.spectre.err.*;


public class FSReader extends InputStream {
    protected InputStream fileIn;

    public FSReader(String aspectBase, String aspectPath, int replayId) 
        throws AspectNotFound {

        String filePath = aspectBase+"/"+aspectPath+"/"+replayId;
        try {
            fileIn = new FileInputStream(filePath);
        } catch(FileNotFoundException ex) {
            throw new AspectNotFound(ex.toString());
        }
    }

    public int available() throws IOException { 
        return fileIn.available();
    }
    public void close() throws IOException {
        fileIn.close();
    }
    public void mark(int readlimit) {
        fileIn.mark(readlimit);
    }
    public boolean markSupported() {
        return fileIn.markSupported();
    }
    public void reset() throws IOException {
        fileIn.reset();
    }
    public long skip(long n) throws IOException {
        return fileIn.skip(n);
    }
    public int read() throws IOException {
        return fileIn.read();
    }
}
