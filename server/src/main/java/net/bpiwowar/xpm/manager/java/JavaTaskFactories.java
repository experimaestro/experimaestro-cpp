package sf.net.experimaestro.manager.java;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * List of factories sharing the same parameters
 */
public class JavaTaskFactories {
    Path[] classpath;
    List<JavaTaskFactory> factories;

    public JavaTaskFactories(Path[] classpath, Collection<JavaTaskFactory> factories) {
        this.classpath = classpath;
        this.factories = new ArrayList<>(factories);
    }

    public JavaTaskFactories() {
    }
}
