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

public class FileFinder {
    private Path basedir;
    private Path filePath;

    public Path find(String basedir, final String filename) throws IOException {
        this.basedir = Paths.get(basedir);
        this.filePath = null;
        final FileFinder parent = this;

        Files.walkFileTree(this.basedir, new SimpleFileVisitor<Path>() {
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs)
                throws IOException {
                if (path.getFileName().toString().equals(filename)) {
                    parent.filePath = path;
                    return FileVisitResult.TERMINATE;
                } else {
                    return FileVisitResult.CONTINUE;
                }
            }
        });

        return this.filePath;
    }
}
