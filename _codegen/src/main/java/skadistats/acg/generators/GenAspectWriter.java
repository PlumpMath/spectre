package skadistats.acg.generators;

import java.util.List;

import org.stringtemplate.v4.*;
import skadistats.acg.util.MsgTypeLoader.MessageType;

public class GenAspectWriter extends BaseGenerator {
    protected List<MessageType> msgTypes;

    public GenAspectWriter(List<MessageType> msgTypes) {
        this.msgTypes = msgTypes;
        this.destPath = BaseGenerator.TARGET_BASE + "/skadistats/spectre/AspectWriter.java";
    }

    public String toString() {
        STGroup tmplGroup = new STGroupFile("templates/AspectWriter.stg", '$', '$');
        ST tmpl = tmplGroup.getInstanceOf("aspect_writer");
        tmpl.add("msgTypes", this.msgTypes);
        return tmpl.render();
    }

}
