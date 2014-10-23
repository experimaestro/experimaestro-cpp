package sf.net.experimaestro.utils.introspection;

import com.google.common.collect.ImmutableMap;
import sf.net.experimaestro.utils.log.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

/**
 * A lightweight class info loader
 */
public class ClassInfoLoader {
    final static private Logger LOGGER = Logger.getLogger();

    final ClassLoader classLoader;

    private final Path[] classpath;

    private final HashMap<String, ClassInfo> classes = new HashMap<>();

    public ClassInfoLoader(Path[] classpath, ClassLoader classLoader) throws IOException {
        this.classpath = classpath.clone();
        for (int i = 0; i < classpath.length; i++) {
            if (Files.isRegularFile(this.classpath[i])) {
                this.classpath[i] = FileSystems.newFileSystem(this.classpath[i].toUri(), ImmutableMap.of(), null)
                        .getPath("/");
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
     *
     * @param name The class name
     * @return The input stream or null if not found
     */
    InputStream getStream(String name) {
        final String path = name.replace(".", "/") + ".class";

        for (Path basepath : classpath) {
            try {
                Path file = basepath.resolve(path);
                if (Files.exists(file)) {
                    return Files.newInputStream(file);
                }
            } catch (IOException e) {
                LOGGER.warn(e, "I/O error while trying to retrieve path %s", path);
            }
        }

        return null;
    }
}
