package skadistats.acg.generators;

import java.io.*;

public class BaseGenerator {
    final static String TARGET_BASE = 
        "aspectSerializer/target/generated-sources/acg/java";
    protected String destPath;

    public void build() throws IOException {
        File destFile = new File(destPath);
        if (!destFile.getParentFile().exists()) {
            destFile.getParentFile().mkdirs();
        }

        OutputStream outFile = new FileOutputStream(this.destPath);
        System.out.println("Generating "+destPath);
        outFile.write(this.toString().getBytes());
        outFile.close();
    }
}
