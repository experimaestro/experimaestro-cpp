package net.bpiwowar.experimaestro.tasks;

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

import com.google.gson.*;
import sf.net.experimaestro.tasks.Path;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Field;

/**
 * Runs an XPM task
 */
public class Runner {
    /**
     * Main method
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {

        // --- Process arguments

        if (args.length != 3) {
            System.err.format("XPM Runner requires three arguments (got %d): class to run, working directory and json parameters%n",
                    args.length);
            System.exit(1);
        }

        final String classname = args[0];
        final String workDirPath = args[1];
        final String jsonInput = args[2];

        // Get class
        Class<? extends AbstractTask> aClass = null;

        try {
            aClass = (Class<? extends AbstractTask>) Runner.class.getClassLoader().loadClass(classname);
        } catch (ClassNotFoundException e) {
            System.err.format("Task %s not found%n", classname);
            System.exit(2);
        }

        // Get working directory
        final File workdir = new File(workDirPath);
        if (!workdir.isDirectory()) {
            System.err.format("Working directory %s is not valid%n", workDirPath);
            System.exit(3);
        }

        // Get json
        final JsonParser jsonParser = new JsonParser();
        JsonObject json = null;
        try {
            json = jsonParser.parse(new FileReader(jsonInput)).getAsJsonObject();
        } catch (FileNotFoundException e) {
            System.err.format("Error while reading JSON file %s: %s%n", jsonInput, e.toString());
            e.printStackTrace(System.err);
            System.exit(4);

        }


        // --- Run
        try {
            XPMTypeAdapterFactory factory = new XPMTypeAdapterFactory();
            TaskDescription taskDescription = aClass.getAnnotation(TaskDescription.class);
            if (taskDescription != null) {
                for(Class<?> registryClass: taskDescription.registry()) {
                    factory.addClass(registryClass);
                }
            }
            final Gson gson = new GsonBuilder()
                    .setExclusionStrategies(new XPMExclusionStrategy())
                    .setFieldNamingStrategy(new XPMNamingStrategy())
                    .registerTypeAdapterFactory(factory)
                    .create();

            // Get the task
            final AbstractTask task = gson.fromJson(json, aClass);

            task.workingDirectory = workdir;

            // Set the @Path annotated fields
            for(Field field: task.getClass().getDeclaredFields()) {
                Path path = field.getAnnotation(Path.class);
                if (path != null) {
                    String name = getString(path.value(), field.getName());
                    boolean accessible = field.isAccessible();
                    if (!accessible) {
                        field.setAccessible(true);
                    }
                    field.set(task, new File(workdir, name));
                    if (!accessible)
                        field.setAccessible(false);
                }
            }

            try {
                task.execute(json);
            } catch (Throwable e) {
                System.err.format("An error occurred while running the task: %s%n", e);
                e.printStackTrace(System.err);
                System.exit(5);
            }

        } catch (Throwable e) {
            System.err.format("An error occurred while configuring the task with JSON: %s%n", e);
            e.printStackTrace(System.err);
            System.exit(5);

        }

    }

    private static String getString(String value, String defaultValue) {
        return "".equals(value) ? defaultValue : value;
    }

    private static class XPMExclusionStrategy implements ExclusionStrategy {
        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            return f.getAnnotation(JsonArgument.class) == null;
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }
    }

    private static class XPMNamingStrategy implements FieldNamingStrategy {
        @Override
        public String translateName(Field f) {
            // Get the name from the annotation
            final JsonArgument annotation = f.getAnnotation(JsonArgument.class);
            if (annotation != null && !annotation.name().equals("")) {
                return annotation.name();
            }

            // Otherwise, use the field name
            return f.getName();
        }
    }
}
