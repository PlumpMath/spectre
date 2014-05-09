package skadistats.spectre;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import skadistats.spectre.err.AspectNotFound;

public class Config {
    Properties prop;

    public Config(String config_path) {
        try {
            InputStream cfg_in = new FileInputStream(config_path);
            this.prop.load(cfg_in);
        } catch (IOException ex) {
            ex.printStackTrace(System.out);
            System.exit(1);
        }
    }

    protected String getBasePath() {
        return prop.getProperty("aspectBasePath");
    }

    public InputStream getAspectInputStream(String key) 
        throws AspectNotFound, IOException {

        if (this.aspectExists(key)) {
            String p = Paths.get(this.getBasePath(), key).toString();
            return new FileInputStream(p);
        } else {
            throw new AspectNotFound(key);
        }
    }

    public OutputStream getAspectOutputStream(String key, Boolean overwrite) 
        throws IOException {

        String p = Paths.get(this.getBasePath(), key).toString();
        return new FileOutputStream(p, overwrite);
    }

    public Boolean aspectExists(String key) {
        Path p = Paths.get(this.getBasePath(), key);
        return p.toFile().exists();
    }
}
