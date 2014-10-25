package sf.net.experimaestro.manager.java;

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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.bpiwowar.experimaestro.tasks.AbstractTask;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileSystemException;
import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.exceptions.ExperimaestroException;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.utils.Introspection;
import sf.net.experimaestro.utils.introspection.ClassInfo;
import sf.net.experimaestro.utils.introspection.ClassInfoLoader;
import sf.net.experimaestro.utils.log.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 *
 */
public class JavaTasksIntrospection {
    public static final String META_INF_PATH = "META-INF/net.bpiwowar.experimaestro/tasks.json";
    final static Logger LOGGER = Logger.getLogger();
    Path[] classpath;

    public JavaTasksIntrospection(Path[] classpath) {
        this.classpath = classpath;
    }

    public static void addToRepository(Repository repository, Connector connector, String[] paths) throws ExperimaestroException, IOException {
        Path[] classpath = Arrays.stream(paths).map(path -> {
            try {
                return connector.getMainConnector().resolveFile(path);
            } catch (FileSystemException e) {
                throw new XPMRuntimeException(e, "Could not resolve path %s", path);
            }
        }).toArray(n -> new Path[n]);

        final JavaTasksIntrospection javaTasksIntrospection = new JavaTasksIntrospection(classpath);
        final ClassInfoLoader classLoader = new ClassInfoLoader(classpath, JavaTasksIntrospection.class.getClassLoader());
        javaTasksIntrospection.addToRepository(repository, classLoader, connector);
    }

    private static void forEachClass(ClassInfoLoader cl, Path[] classpath, BiFunction<ClassInfo, Description, ?> f) throws IOException, ExperimaestroException {

        for (Path base : classpath) {
            final Path infoFile = base.resolve(META_INF_PATH);
            if (!Files.exists(infoFile)) {
                continue;
            }

            Type collectionType = new TypeToken<Description>() {
            }.getType();
            final Description description;
            try {
                final Gson gson = new GsonBuilder()
                        .create();
                final InputStreamReader reader = new InputStreamReader(Files.newInputStream(infoFile));
                description = gson.fromJson(reader, collectionType);
            } catch (IllegalStateException e) {
                throw new ExperimaestroException(e, "Could not read json file %s", infoFile)
                        .addContext("while inspecting resource %s", base);
            }


            final Consumer<Introspection.ClassFile> action = t -> {
                try {
                    final ClassInfo classInfo = new ClassInfo(cl, t.file);
                    if (classInfo.belongs(AbstractTask.class)) {
                        f.apply(classInfo, description);
                    }
                } catch (IOException e) {
                    throw new XPMRuntimeException(e);
                }
            };

            // Get forEachClass through package inspection
            if (description.packages != null) {
                for (String name : description.packages) {
                    Introspection.findClasses(base, 1, name).forEach(action);
                }
            }

            // Get forEachClass directly
            if (description.classes != null) {
                for (String name : description.classes) {
                    final Path fileObject = base.resolve(name.replace('.', '/'));
                    action.accept(new Introspection.ClassFile(fileObject, name));
                }
            }
        }
    }

    private void addToRepository(Repository repository, ClassInfoLoader cl, Connector connector) throws ExperimaestroException, IOException {
        BiFunction<ClassInfo, Description, ?> f = (classInfo, description) -> {
            // Creates the task factory
            JavaTaskFactory factory = new JavaTaskFactory(this, connector, repository, classInfo, description.namespaces);
            repository.addFactory(factory);
            return true;
        };

        // FIXME: switch to this one
        forEachClass(cl, classpath, f);

    }

    /**
     * Json description
     */
    static class Description {
        Map<String, String> namespaces;
        ArrayList<String> packages;
        ArrayList<String> classes;
    }


}
