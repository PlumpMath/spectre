package skadistats.spectre;

import skadistats.spectre.AspectReader;
import skadistats.spectre.AspectWriter;

public class Spectre {
    protected String baseDir;

    public Spectre(String baseDir) {
        this.baseDir = baseDir;
    }

    public AspectReader newReader(String aspectPath, int replayId) {
        return AspectReader.newReader(this.baseDir, aspectPath, replayId);
    }

    public AspectWriter newWriter(String aspectPath, int replayId) {
        return AspectWriter.newWriter(this.baseDir, aspectPath, replayId);
    }
}
