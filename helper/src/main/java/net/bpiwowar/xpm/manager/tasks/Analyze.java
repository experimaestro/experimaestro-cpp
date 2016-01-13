/*
 *
 *  * This file is part of experimaestro.
 *  * Copyright (c) 2015 B. Piwowarski <benjamin@bpiwowar.net>
 *  *
 *  * experimaestro is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * experimaestro is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package net.bpiwowar.xpm.manager.tasks;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import net.bpiwowar.xpm.manager.Constants;
import net.bpiwowar.xpm.utils.introspection.ClassInfo;
import net.bpiwowar.xpm.utils.introspection.ClassInfoLoader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static java.lang.String.format;

/**
 * Analyze and save the java tasks contained in a project
 * <p>
 * Arguments: [outputfile]
 * Arguments are the classpath
 */
public class Analyze {
    final static private Logger LOGGER = Logger.getLogger("Analyze");
    public static final Type JAVA_INFORMATION_TYPE = new TypeToken<ArrayList<JavaTaskInformation>>() {
    }.getType();
    public static final Type SCRIPT_INFORMATION_TYPE = new TypeToken<ArrayList<ScriptTaskInformation>>() {
    }.getType();

    public static void main(String[] args) throws IOException {
        if (args.length != 1 && args.length != 2) {
            System.err.format("Arguments: <output path> [<classpath>]; error: %d arguments given%n", args.length);
            System.exit(1);
        }

        Path output = new File(args[0]).toPath();
        LOGGER.info(format("Outputing to %s", output));

        if (args.length == 1) {
            analyze(output, System.getProperty("java.class.path"));
        } else {
            analyze(output, args[1]);
        }
    }

    private static void analyze(Path output, String classpathstring) throws IOException {
        LOGGER.fine(format("Classpath is %s", classpathstring));
        String[] classpathEntries = classpathstring.split(File.pathSeparator);
        Path[] classpath = new Path[classpathEntries.length];
        for (int i = 0; i < classpathEntries.length; i++) {
            classpath[i] = new File(classpathEntries[i]).toPath();
        }

        final ClassInfoLoader classInfoLoader = new ClassInfoLoader(classpath, ClassLoader.getSystemClassLoader());
        ArrayList<TaskInformation> informations = new ArrayList<>();
        BiFunction<ClassInfo, Description, ?> f = (classInfo, description) -> {
            // Creates the task factory
            TaskInformation information = new JavaTaskInformation(classInfo, description.namespaces);
            informations.add(information);
            return true;
        };

        forEachClass(classInfoLoader, classInfoLoader.getClasspath(), f, true);

        final Gson gson = new GsonBuilder().create();

        try (final BufferedWriter writer = Files.newBufferedWriter(output);
             final JsonWriter jsonWriter = new JsonWriter(writer)) {
            gson.toJson(informations, JAVA_INFORMATION_TYPE, jsonWriter);
        }
    }

    /**
     * Json description
     */
    static public class Description {
        public Map<String, String> namespaces;
        ArrayList<String> packages;
        ArrayList<String> classes;
    }

    /**
     * Introspection of a list of jars
     * @param cl Class information loader
     * @param classpath The classpath
     * @param f The callback function
     * @param searchAll If false, the class information will not be looked at when crossing jar boundaries
     * @throws IOException If something goes wrong
     */
    public static void forEachClass(ClassInfoLoader cl, Path[] classpath, BiFunction<ClassInfo, Description, ?> f, boolean searchAll) throws IOException {
        for (Path base : classpath) {
            LOGGER.fine("Looking at " + base.resolve(Constants.JAVATASK_INTROSPECTION_PATH).toUri() + " in " + base.toUri());
            final Path infoFile = base.resolve(Constants.JAVATASK_INTROSPECTION_PATH);
            if (!Files.exists(infoFile)) {
                continue;
            }

            LOGGER.info(format("Found introspection file: %s", infoFile.toUri()));
            load(cl, f, searchAll ? classpath : new Path[]{base}, infoFile);
        }
    }

    /**
     * Introspection of a given jar file
     *
     * @param cl The class information loader
     * @param f The function that will be called each time a Java task is found
     * @param searched List of jars to be inspected
     * @param infoFile The introspection JSON file giving the list of packages / classes to inspect in the jars
     * @throws IOException
     */
    private static void load(ClassInfoLoader cl, BiFunction<ClassInfo, Description, ?> f, Path[] searched, Path infoFile) throws IOException {
        Type collectionType = new TypeToken<Description>() {
        }.getType();
        final Description description;
        try {
            final Gson gson = new GsonBuilder()
                    .create();
            final InputStreamReader reader = new InputStreamReader(Files.newInputStream(infoFile));
            description = gson.fromJson(reader, collectionType);
        } catch (IllegalStateException e) {
            throw new RuntimeException(format("Could not read json file %s while inspecting resource %s", infoFile, searched), e);
        }


        final Consumer<Introspection.ClassFile> action = t -> {
            try {
                final ClassInfo classInfo = new ClassInfo(cl, t.file);
                if (classInfo.belongs(AbstractTask.class)) {
                    f.apply(classInfo, description);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        // Get forEachClass through package inspection
        if (description.packages != null) {
            for (String name : description.packages) {
                for (Path path : searched) {
                    LOGGER.fine(format("Searching %s in path %s", name, path));
                    Introspection.findClasses(path, 1, name).forEach(action);
                }
            }
        }

        // Get forEachClass directly
        if (description.classes != null) {
            for (String name : description.classes) {
                for (Path path : searched) {
                    final Path fileObject = path.resolve(name.replace('.', '/'));
                    LOGGER.fine(format("Searching package %s in path %s", name, fileObject));
                    Introspection.findClasses(path, 1, name).forEach(action);
                    action.accept(new Introspection.ClassFile(fileObject, name));
                }
            }
        }
    }
}
