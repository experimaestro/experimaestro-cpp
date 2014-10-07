package sf.net.experimaestro.utils.introspection;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.NameScope;
import sf.net.experimaestro.utils.introspection.ClassInfo;
import sf.net.experimaestro.utils.log.Logger;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;

/**
 * A lightweight class info loader
 */
public class ClassInfoLoader {
    final static private Logger LOGGER = Logger.getLogger();

    private final FileObject[] classpath;
    final ClassLoader classLoader;
    private final HashMap<String, ClassInfo> classes = new HashMap<>();

    public ClassInfoLoader(FileObject[] classpath, FileSystemManager vfsManager, ClassLoader classLoader) throws FileSystemException {
        this.classpath = classpath.clone();
        for(int i = 0; i < classpath.length; i++) {
            if (vfsManager.canCreateFileSystem(this.classpath[i])) {
                this.classpath[i] = vfsManager.createFileSystem(this.classpath[i]);
            }

        }
        this.classLoader = classLoader;
    }

    public ClassInfo get(String name) {
        ClassInfo classInfo = classes.get(name);
        if (classInfo == null)
            classes.put(name, classInfo = new ClassInfo(this, name));
        return classInfo;
    }


    /**
     * Get a stream of the object represented by the name
     * @param name The class name
     * @return The input stream or null if not found
     */
    InputStream getStream(String name) {
        final String path = name.replace(".", "/") + ".class";

        for(FileObject basepath: classpath) {
            try {
                FileObject file = basepath.resolveFile(path, NameScope.DESCENDENT_OR_SELF);
                if (file.exists()) {
                    return file.getContent().getInputStream();
                }
            } catch (FileSystemException e) {
                LOGGER.warn(e, "I/O error while trying to retrieve path %s", path);
            }
        }

        return null;
    }
}
