package skadistats.spectre;

import java.io.IOException;

import skadistats.spectre.AspectReader;
import skadistats.spectre.AspectWriter;
import skadistats.spectre.persist.s3.S3Reader;
import skadistats.spectre.persist.s3.S3Writer;

import skadistats.spectre.err.*;

public class Spectre {
    protected String s3Bucket;

    public Spectre(String s3Bucket) {
        this.s3Bucket = s3Bucket;
    }

    public AspectReader newReader(String aspectPath, int replayId) 
        throws AspectNotFound, IOException {

        return AspectReader.newReader(new S3Reader(s3Bucket, aspectPath, replayId), aspectPath);
    }

    public AspectWriter newWriter(String aspectPath, int replayId) 
        throws AspectNotFound, IOException {

        return AspectWriter.newWriter(new S3Writer(s3Bucket, aspectPath, replayId), aspectPath);
    }
}
