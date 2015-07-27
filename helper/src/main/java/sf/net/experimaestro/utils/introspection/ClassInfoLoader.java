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
import java.util.HashMap;
import java.util.Map;

/**
 * A lightweight class info loader
 */
public class ClassInfoLoader {
    final ClassLoader classLoader;

    private final Path[] classpath;

    private final HashMap<String, ClassInfo> classes = new HashMap<>();

    public ClassInfoLoader(Path[] classpath, ClassLoader classLoader) throws IOException {
        this.classpath = classpath.clone();
        for (int i = 0; i < classpath.length; i++) {
            // Transform files so as to use the JAR FileSystem on top of it
            if (Files.isRegularFile(this.classpath[i])) {
                final URI uri;
                try {
                    uri = new URI("jar:" + this.classpath[i].toUri().toString());
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
                try {
                    this.classpath[i] = FileSystems.getFileSystem(uri).getPath("/");
                } catch (FileSystemNotFoundException e) {
                    try {
                        Map<String, ?> env = new HashMap<>();
                        this.classpath[i] = FileSystems.newFileSystem(uri, env, null).getPath("/");
                    } catch(Throwable e2) {
                        throw new IOException("Could not create ZIP filesystem with " + uri + ": " + e2);
                    }
                }
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
//                LOGGER.warn(e, "I/O error while trying to retrieve path %s", path);
            }
        }

        return null;
    }
}