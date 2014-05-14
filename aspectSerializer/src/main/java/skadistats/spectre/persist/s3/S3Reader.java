package skadistats.spectre.persist.s3;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;

public class S3Reader extends InputStream {
    protected AmazonS3Client client;
    protected S3AspectConfig cfg;

    public S3Reader(AWSCredentials s3Creds, ClientConfiguration s3Cfg, S3AspectConfig aspectCfg) {
        this.client = new AmazonS3Client(s3Creds, s3Cfg);
        this.cfg    = aspectCfg;
    }
}
