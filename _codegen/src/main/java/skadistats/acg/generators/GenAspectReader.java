package skadistats.acg.generators;

import java.util.List;

import org.stringtemplate.v4.*;
import skadistats.acg.util.MsgTypeLoader.MessageType;

public class GenAspectReader extends BaseGenerator {
    protected List<MessageType> msgTypes;

    public GenAspectReader(List<MessageType> msgTypes) {
        this.msgTypes = msgTypes;
        this.destPath = BaseGenerator.TARGET_BASE + "/skadistats/spectre/AspectReader.java";
    }

    public String toString() {
        STGroup tmplGroup = new STGroupFile("templates/AspectReader.stg", '$', '$');
        ST tmpl = tmplGroup.getInstanceOf("aspect_reader");
        tmpl.add("msgTypes", this.msgTypes);
        return tmpl.render();
    }

}
