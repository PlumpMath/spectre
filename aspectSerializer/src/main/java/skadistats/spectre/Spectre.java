package skadistats.spectre;

import java.io.IOException;

import skadistats.spectre.AspectReader;
import skadistats.spectre.AspectWriter;
import skadistats.spectre.persist.fs.FSReader;
import skadistats.spectre.persist.fs.FSWriter;

import skadistats.spectre.err.*;

public class Spectre {
    protected String baseDir;

    public Spectre(String baseDir) {
        this.baseDir = baseDir;
    }

    public AspectReader newReader(String aspectPath, int replayId) 
        throws AspectNotFound, IOException {

        return AspectReader.newReader(new FSReader(baseDir, aspectPath, replayId), aspectPath);
    }

    public AspectWriter newWriter(String aspectPath, int replayId) 
        throws AspectNotFound, IOException {

        return AspectWriter.newWriter(new FSWriter(baseDir, aspectPath, replayId), aspectPath);
    }
}
