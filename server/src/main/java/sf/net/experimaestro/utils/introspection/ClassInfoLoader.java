package sf.net.experimaestro.utils.introspection;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import sf.net.experimaestro.utils.introspection.ClassInfo;

import java.util.HashMap;

/**
 * A lightweight class info loader
 */
public class ClassInfoLoader {
    private final FileObject[] classpath;
    private final FileSystemManager vfsManager;
    private final ClassLoader classLoader;
    private final HashMap<String, ClassInfo> classes = new HashMap<>();

    public ClassInfoLoader(FileObject[] classpath, FileSystemManager vfsManager, ClassLoader classLoader) {
        this.classpath = classpath;
        this.vfsManager = vfsManager;
        this.classLoader = classLoader;
    }

    public ClassInfo get(String name) {
        ClassInfo classInfo = classes.get(name);
        if (classInfo == null)
            classes.put(name, classInfo = new ClassInfo(name));
        return classInfo;
    }
}
