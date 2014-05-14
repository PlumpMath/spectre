package skadistats.spectre.persist;

public class S3Persister {

    public static class S3Reader extends InputStream {
        public S3Reader(String bucket, String prefix, String aspectPath, int replayId) {
            super();
        }

        public int read() throws IOException {
        }
    }

    public static class S3Writer extends OutputStream {
        public S3Writer(String bucket, String prefix, String aspectPath, int replayId) {
            super();
        }
    }

    public writeMsg(int msgType, byte[] msgData) throws IOException {
        DataOutputStream dout = new DataOutputStream(this.out);
        // write msg_type
        Varint.writeUnsignedVarInt(msgType, dout);
        // write data
        dout.write(data);
    }
}
