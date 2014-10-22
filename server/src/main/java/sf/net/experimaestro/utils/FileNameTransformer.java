package sf.net.experimaestro.utils;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;

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

    public FileObject transform(FileObject file) throws FileSystemException {
        final String baseName = prefix + file.getName().getBaseName() + suffix;
        return file.getParent().resolveFile(baseName);
    }
}
