package skadistats.spectre.persist;

public class S3Persister implements Persister {

    public writeMsg(int msgType, byte[] msgData) throws IOException {
        DataOutputStream dout = new DataOutputStream(this.out);
        // write msg_type
        Varint.writeUnsignedVarInt(msgType, dout);
        // write data
        dout.write(data);
    }
}
