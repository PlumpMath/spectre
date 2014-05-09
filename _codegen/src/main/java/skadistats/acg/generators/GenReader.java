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

        tmpl.add("pkgName", "skadistats.spectre"+msgType.aspectPath.replace('/', '.'));

        String clsPath = "skadistats.spectre.proto";
        clsPath += "."+msgType.aspectPath.split("/")[1];
        clsPath += "."+msgType.msgCls;
        tmpl.add("clsPath", clsPath);

        try {
            tmpl.add("clsName", msgType.msgCls.split("\\.")[1]);
        } catch(ArrayIndexOutOfBoundsException ex) {
            System.err.println("msgCls = "+msgType.msgCls);
            String[] s = msgType.msgCls.split("\\.");
            for (int i=0; i<s.length; i++) {
                System.err.println("msgCls["+i+"] = "+s[i]);
            }
            throw ex;
        }

        return tmpl.render();
    }

}
