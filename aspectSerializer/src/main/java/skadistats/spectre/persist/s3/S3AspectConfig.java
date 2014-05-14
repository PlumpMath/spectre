package skadistats.spectre.persist.s3;

public class S3AspectConfig {
    public String bucketName;
    public String keyPrefix;
    public String aspectPath;
    public int    replayId;

    public S3AspectConfig(String bucketName, String keyPrefix, String aspectPath, int replayId) {
        this.bucketName = bucketName;
        this.keyPrefix  = keyPrefix;
        this.aspectPath = aspectPath;
        this.replayId   = replayId;
    }
}
