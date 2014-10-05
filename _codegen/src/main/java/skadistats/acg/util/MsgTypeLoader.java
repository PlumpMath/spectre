package skadistats.acg.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import java.util.Properties;

import skadistats.acg.util.FileFinder;

public class MsgTypeLoader {
    final static String PROP_BASE = "aspectSerializer";
    final static String PROP_NAME = "msgtypes.properties";

    static public class InvalidMessageDeclaration extends RuntimeException {
        public InvalidMessageDeclaration(String msg) {super(msg);}
    }

    static public class MessageType {
        public int    msgId;
        public String aspectPath;
        public String protoPkg;
        public String protoFullCls;
        public String protoCls;
        public String readWritePkg;
        public String toString() { return protoFullCls+":"+msgId+":"+aspectPath; }
    }

    private MessageType parseProperty(Map.Entry<Object,Object> entry) {
        String key = (String)entry.getKey();
        String val = (String)entry.getValue();

        String[] sval = val.split(":");
        if (sval.length == 2) {
            try {
                MessageType msg = new MessageType();
                msg.msgId  = Integer.valueOf(sval[0].trim());
                msg.aspectPath = sval[1].trim();

                msg.protoFullCls = key.trim();
                msg.protoCls = msg.protoFullCls.split("\\.")[1];
                msg.protoPkg = "skadistats.spectre.proto"
                             + "."+msg.aspectPath.split("/")[1];

                msg.readWritePkg = "skadistats.spectre"+msg.aspectPath.replace('/','.');
                return msg;
            } catch(RuntimeException ex) {
                throw new InvalidMessageDeclaration(ex+" | "+key+" : "+val);
            }
        } else {
            throw new InvalidMessageDeclaration(key+" : "+val);
        }
    }

    public List<MessageType> getMsgTypes() throws IOException {
        Path propFilePath = new FileFinder().find(this.PROP_BASE, this.PROP_NAME);

        if (propFilePath == null) {
            throw new RuntimeException("Could not find "+this.PROP_NAME+" file");
        } else {
            System.out.println("Reading message types from: "+propFilePath);

            Properties prop = new Properties();
            InputStream in = new FileInputStream(propFilePath.toString());
            prop.load(in);
            in.close();
            
            List<MessageType> types = new LinkedList<MessageType>();
            for (Map.Entry<Object,Object> entry : prop.entrySet()) {
                MessageType mtype = this.parseProperty(entry);
                if (!mtype.aspectPath.equals("/internal"))
                    types.add(mtype);
            }
            return types;
        }
    }
}
