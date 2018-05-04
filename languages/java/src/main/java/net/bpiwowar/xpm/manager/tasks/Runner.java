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


import net.bpiwowar.std.StringList;
import net.bpiwowar.xpm.Register;

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
        // The XPM register
        Register register = new Register();

        // Populate the register
        register.parse(new StringList(args));
//
//        // --- Process arguments
//
//        if (args.length < 3) {
//            System.err.format("XPM runner requires three or more arguments:%n");
//            System.err.format(" 1. The class to run%n");
//            System.err.format(" 2. The working directory%n");
//            System.err.format(" 3. A JSON file%n");
//            System.err.format(" 4+. JSON arguments%n");
//            System.exit(1);
//        }
//
//        final String classname = args[0];
//        final String workDirPath = args[1];
//        final String jsonInput = args[2];
//
//        // Get class
//        Class<? extends AbstractTask> aClass = null;
//
//        try {
//            aClass = (Class<? extends AbstractTask>) Runner.class.getClassLoader().loadClass(classname);
//        } catch (ClassNotFoundException e) {
//            System.err.format("Task %s not found%n", classname);
//            System.exit(2);
//        }
//
//        // Get working directory
//        final File workdir = new File(workDirPath);
//        if (!workdir.isDirectory()) {
//            System.err.format("Working directory %s is not valid%n", workDirPath);
//            System.exit(3);
//        }
//
//        // Get json
//        final JsonParser jsonParser = new JsonParser();
//        JsonObject json = null;
//        if (!(".".equals(jsonInput))) {
//            try {
//                json = jsonParser.parse(new FileReader(jsonInput)).getAsJsonObject();
//            } catch (FileNotFoundException e) {
//                System.err.format("Error while reading JSON file %s: %s%n", jsonInput, e.toString());
//                e.printStackTrace(System.err);
//                System.exit(4);
//            }
//        } else {
//            json = new JsonObject();
//        }
//
//        // Read the remaining arguments
//        // --qualified-name JSON value
//        // e.g
//        for (int i = 3; i < args.length; i += 2) {
//            String[] fields = args[i].split("\\.");
//            JsonPointer pointer = new JsonSelfPointer(json);
//
//            for (int j = 0; j < fields.length; j++) {
//                String field = fields[j].replace("\\.", ".");
//                if (field.equals("[]")) {
//                    pointer = new JsonArrayPointer(pointer);
//                } else {
//                    pointer = new JsonObjectPointer(pointer, field);
//                }
//            }
//
//            JsonElement value = jsonParser.parse(args[i + 1]);
//            pointer.set(value);
//        }
//
//        // --- Run
//        try {
//            XPMTypeAdapterFactory factory = new XPMTypeAdapterFactory();
//            TaskDescription taskDescription = aClass.getAnnotation(TaskDescription.class);
//            if (taskDescription != null) {
//                for (Class<?> registryClass : taskDescription.registry()) {
//                    factory.addClass(registryClass);
//                }
//            }
//            final GsonBuilder gsonBuilder = new GsonBuilder()
//                    .setExclusionStrategies(new XPMExclusionStrategy())
//                    .setFieldNamingStrategy(new XPMNamingStrategy())
//                    .registerTypeAdapterFactory(factory);
//            final Gson gson = gsonBuilder
//                    .create();
//
//            // Get the task
//            final AbstractTask task = gson.fromJson(json, aClass);
//            task.gsonBuilder = gsonBuilder;
//            task.workingDirectory = workdir;
//
//            // Set the @Path annotated fields
//            for (Field field : task.getClass().getDeclaredFields()) {
//                JsonPath path = field.getAnnotation(JsonPath.class);
//                if (path != null) {
//                    String name = getString(path.value(), field.getName());
//                    boolean accessible = field.isAccessible();
//                    if (!accessible) {
//                        field.setAccessible(true);
//                    }
//                    field.set(task, new File(workdir, name));
//                    if (!accessible)
//                        field.setAccessible(false);
//                }
//            }
//
//            try {
//                task.progressListener = new ProgressListener();
//                task.execute(json);
//                System.exit(0);
//            } catch (Throwable e) {
//                System.err.format("An error occurred while running the task: %s%n", e);
//                e.printStackTrace(System.err);
//                System.exit(5);
//            }
//
//        } catch (Throwable e) {
//            System.err.format("An error occurred while configuring the task with JSON: %s%n", e);
//            e.printStackTrace(System.err);
//            System.exit(5);
//
//        }

    }


}
