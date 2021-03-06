package skadistats.acg.generators;

import java.util.List;

import org.stringtemplate.v4.*;
import skadistats.acg.generators.BaseGenerator;
import skadistats.acg.util.MsgTypeLoader.MessageType;

public class GenReader extends BaseGenerator {
    protected MessageType msgType;

    public GenReader(MessageType msgType) {
        this.msgType = msgType;
        this.destPath = BaseGenerator.TARGET_BASE + "/skadistats/spectre";
        this.destPath += msgType.aspectPath+"/Reader.java";
    }

    public String toString() {
        STGroup tmplGroup = new STGroupFile("templates/MsgReader.stg", '$', '$');
        ST tmpl = tmplGroup.getInstanceOf("msg_reader");
        tmpl.add("msgType", msgType);
        return tmpl.render();
    }

}
