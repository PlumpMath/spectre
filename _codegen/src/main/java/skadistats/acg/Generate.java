package skadistats.acg;

import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import skadistats.acg.util.MsgTypeLoader;
import skadistats.acg.util.MsgTypeLoader.MessageType;

import skadistats.acg.generators.*;


public class Generate {
    public static void main(String[] args) throws IOException {
        MsgTypeLoader mtLoader = new MsgTypeLoader();
        List<MessageType> msgTypes = mtLoader.getMsgTypes();

        new GenAspectReader(msgTypes).build();
        new GenAspectWriter(msgTypes).build();

        for (MessageType msgType : msgTypes) {
            new GenReader(msgType).build();
        }
            
    }
}
