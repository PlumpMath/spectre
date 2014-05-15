package skadistats.spectre.persist;

import java.io.IOException;
import java.util.List;
import skadistats.spectre.persist.Helpers.MessageEnvelope;


public interface AspectDeserializer {
    public int getReplayId() throws IOException;
    public List<String> getStringTable() throws IOException;

    public boolean hasNext();
    public MessageEnvelope readMsg() throws IOException;
}
