package skadistats.spectre.persist;

public interface Persister {

    public writeMsg(int msgType, byte[] msgData) throws IOException;
    public readMsg
}
