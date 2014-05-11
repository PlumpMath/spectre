package skadistats.acg.generators;

import java.util.List;

import org.stringtemplate.v4.*;
import skadistats.acg.generators.BaseGenerator;
import skadistats.acg.util.MsgTypeLoader.MessageType;

public class GenWriter extends BaseGenerator {
    protected MessageType msgType;

    public GenWriter(MessageType msgType) {
        this.msgType = msgType;
        this.destPath = BaseGenerator.TARGET_BASE + "/skadistats/spectre";
        this.destPath += msgType.aspectPath+"/Writer.java";
    }

    public String toString() {
        STGroup tmplGroup = new STGroupFile("templates/MsgWriter.stg", '$', '$');
        ST tmpl = tmplGroup.getInstanceOf("msg_writer");
        tmpl.add("msgType", msgType);
        return tmpl.render();
    }

}
