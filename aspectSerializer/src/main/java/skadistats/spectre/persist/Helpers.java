package skadistats.spectre.persist;


public class Helpers {

    public static class MessageEnvelope {
        public int    tick;
        public int    msgType;
        public byte[] msgData;
        public MessageEnvelope(int tick, int msgType, byte[] msgData) {
            this.tick    = tick;
            this.msgType = msgType;
            this.msgData = msgData;
        }
    }
}
