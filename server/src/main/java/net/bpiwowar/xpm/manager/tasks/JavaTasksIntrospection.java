package net.bpiwowar.xpm.manager.tasks;

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

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.bpiwowar.xpm.exceptions.ExperimaestroException;
import net.bpiwowar.xpm.exceptions.XPMScriptRuntimeException;
import net.bpiwowar.xpm.manager.Constants;
import net.bpiwowar.xpm.manager.Repository;
import net.bpiwowar.xpm.manager.scripting.ScriptContext;
import net.bpiwowar.xpm.utils.GsonConverter;
import net.bpiwowar.xpm.utils.gson.JsonPathAdapter;
import net.bpiwowar.xpm.utils.introspection.ClassInfo;
import net.bpiwowar.xpm.utils.introspection.ClassInfoLoader;
import net.bpiwowar.xpm.utils.log.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiFunction;

/**
 *
 */
public class JavaTasksIntrospection {
    final static Logger LOGGER = Logger.getLogger();
    public static final String XPM_JSON_FILENAME = "xpm.json";
    Path[] classpath;

    public JavaTasksIntrospection(Path[] classpath) {
        this.classpath = classpath;
    }

    public static void addToRepository(Repository repository, Path[] classpath, Path cachepath) throws ExperimaestroException, IOException {
        // First check if cache is OK
        if (cachepath != null) {
            boolean validCache = Files.exists(cachepath);

            // Check based on modification time
            if (validCache) {
                FileTime cacheTime = Files.getLastModifiedTime(cachepath);
                for (Path path : classpath) {
                    try {
                        FileTime modifiedTime = Files.getLastModifiedTime(path);
                        if (cacheTime.compareTo(modifiedTime) <= 0) {
                            validCache = false;
                            break;
                        }
                    } catch (NoSuchFileException e) {
                        LOGGER.warn("File not found: %s", path);
                    }
                }
            }

            if (validCache) {
                final Gson gson = GsonConverter.defaultBuilder.create();

                try (BufferedReader in = Files.newBufferedReader(cachepath)) {
                    JavaTaskFactories factories = gson.fromJson(in, JavaTaskFactories.class);
                    factories.factories.forEach(f -> {
                        repository.addFactory(f);
                        f.setJavaCommandBuilder(new JavaCommand(factories.classpath));
                    });
                    return;
                } catch (Throwable t) {
                    ScriptContext.get().getLogger("javatask").error("Error while reading cache file (%s) -- regenerating", t.toString());
                }
            }

        }

        // Run
        final JavaTasksIntrospection javaTasksIntrospection = new JavaTasksIntrospection(classpath);
        final ClassInfoLoader classLoader = new ClassInfoLoader(classpath, JavaTasksIntrospection.class.getClassLoader());
        Collection<JavaTaskFactory> list = javaTasksIntrospection.addToRepository(repository, classLoader);

        if (cachepath != null) {
            final Gson gson = GsonConverter.defaultBuilder.create();
            try (BufferedWriter writer = Files.newBufferedWriter(cachepath)) {
                gson.toJson(new JavaTaskFactories(classpath, list), writer);
            }
        }
    }


    private Collection<JavaTaskFactory> addToRepository(Repository repository, ClassInfoLoader cl) throws ExperimaestroException, IOException {
        ArrayList<JavaTaskFactory> factories = new ArrayList<>();
        BiFunction<ClassInfo, Analyze.Description, ?> f = (classInfo, description) -> {
            // Creates the task factory
            JavaTaskInformation information = new JavaTaskInformation(classInfo, description.namespaces);
            JavaTaskFactory factory = new JavaTaskFactory(new JavaCommand(classpath), repository, information);
            repository.addFactory(factory);
            factories.add(factory);
            return true;
        };

        Analyze.forEachClass(cl, classpath, f, false);
        return factories;
    }

    private static void addJarToRepository(Repository repository, Path jarPath, JavaCommandBuilder builder) throws IOException {
        URI uri = URI.create("jar:" + jarPath.toUri() + "!/");
        try (FileSystem fs = FileSystems.newFileSystem(uri, ImmutableMap.of())) {
            final Path path = Paths.get(uri);
            final Path taskPath = path.resolve(Constants.JAVATASK_TASKS_PATH);
            if (!Files.isRegularFile(taskPath)) {
                throw new XPMScriptRuntimeException("Could not find file %s", path);
            }
            final Gson gson = new GsonBuilder().create();
            try (final BufferedReader reader = Files.newBufferedReader(taskPath)) {
                final ArrayList<JavaTaskInformation> list = gson.fromJson(reader, Analyze.INFORMATION_TYPE);
                for (JavaTaskInformation information : list) {
                    // Creates the task factory
                    JavaTaskFactory factory = new JavaTaskFactory(builder, repository, information);
                    repository.addFactory(factory);
                }
            }
        }
    }

    public static void addJarToRepository(Repository repository, Path jarPath) throws IOException {
        Path[] classpath = new Path[]{jarPath};
        addJarToRepository(repository, jarPath, new JavaCommand(classpath));
    }

    /**
     * Load either a java task repository from a JAR file or a directory containing a xpm.json file
     *
     * @param repository
     * @param jarPath
     * @throws IOException
     */
    public static void scanRepository(Repository repository, Path jarPath) throws IOException {
        if (Files.isRegularFile(jarPath)) {
            // If this is a file, we suppose it is a JAR file
            addJarToRepository(repository, jarPath);
        } else {
            // Find the xpm.json file
            Path jsonPath = jarPath.resolve(XPM_JSON_FILENAME);
            final GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(Path.class, new JsonPathAdapter(jarPath));
            final Gson gson = builder.create();

            try (final BufferedReader reader = Files.newBufferedReader(jsonPath)) {
                final JavaSpecification spec = gson.fromJson(reader, JavaSpecification.class);
                for (Path path : spec.jars) {
                    path = jarPath.resolve(path);
                    if (spec.binaries == null) {
                        throw new IOException("No binaries in JSON " + jsonPath);
                    }
                    addJarToRepository(repository, path, new JavaCommandLauncher(spec.binaries));
                }

            }
        }
    }

}
