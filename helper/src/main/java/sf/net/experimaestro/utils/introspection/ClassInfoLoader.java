package sf.net.experimaestro.utils.introspection;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
 *
 * experimaestro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * experimaestro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A lightweight class info loader
 */
public class ClassInfoLoader {
    final static private Logger LOGGER = Logger.getLogger("ClassInfoLoader");

    final ClassLoader classLoader;

    private final Path[] classpath;

    private final HashMap<String, ClassInfo> classes = new HashMap<>();

    public ClassInfoLoader(Path[] classpath, ClassLoader classLoader) throws IOException {
        ArrayList<Path> _classpath = new ArrayList<>();
        for (int i = 0; i < classpath.length; i++) {
            Path aClasspath = classpath[i];
            // Transform files so as to use the JAR FileSystem on top of it
            if (Files.isRegularFile(aClasspath)) {
                final URI uri;
                try {
                    uri = new URI("jar:" + aClasspath.toUri().toString());
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
                try {
                    aClasspath = FileSystems.getFileSystem(uri).getPath("/");
                    _classpath.add(aClasspath);
                } catch (FileSystemNotFoundException e) {
                    try {
                        Map<String, ?> env = new HashMap<>();
                        aClasspath = FileSystems.newFileSystem(uri, env, null).getPath("/");
                        LOGGER.fine("Created a ZIP filesystem with " + aClasspath.toUri());
                        _classpath.add(aClasspath);
                    } catch(UnsupportedOperationException e3) {
                        LOGGER.warning("Could not create ZIP filesystem with " + uri + ": " + e3);
                    } catch(Throwable e2) {
                        throw new IOException("Could not create ZIP filesystem with " + uri + ": " + e2);
                    }
                }
            } else if (Files.isDirectory(aClasspath)) {
                _classpath.add(aClasspath);
            } else {
                LOGGER.warning("Ignoring classpath " + aClasspath + ": not a directory or a file");
            }
        }

        this.classpath = _classpath.toArray(new Path[_classpath.size()]);
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
            if (basepath == null) continue;
            try {
                Path file = basepath.resolve(path);
                if (Files.exists(file)) {
                    return Files.newInputStream(file);
                }
            } catch (IOException e) {
//                LOGGER.warn(e, "I/O error while trying to retrieve path %s", path);
            }
        }

        return null;
    }

    public Path[] getClasspath() {
        return classpath;
    }
}
