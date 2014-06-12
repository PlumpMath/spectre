package skadistats.spectre.persist;

import java.io.IOException;
import java.io.DataOutputStream;
import java.util.List;
import skadistats.spectre.persist.Helpers.MessageEnvelope;

public interface AspectSerializer {
    public void setReplayId(int replayId) throws IOException;
    public void writeMsg(MessageEnvelope msgEnv) throws IOException;
    public void writeStringTable(List<String> strTable) throws IOException;
}
