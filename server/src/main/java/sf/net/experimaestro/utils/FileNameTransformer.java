package sf.net.experimaestro.utils;

import java.nio.file.Path;
import java.nio.file.FileSystemException;

/**
 * Transforms the filename
 */
final public class FileNameTransformer {
    final String prefix;
    final String suffix;

    public FileNameTransformer(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public Path transform(Path path) throws FileSystemException {
        final String baseName = prefix + path.getFileName().toString() + suffix;
        return path.getParent().resolve(baseName);
    }
}
