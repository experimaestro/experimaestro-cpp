package net.bpiwowar.xpm.manager.tasks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.bpiwowar.xpm.manager.Repository;
import net.bpiwowar.xpm.utils.gson.JsonPathAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 *
 */
public class TasksLoader {
    /**
     * Load a tasks repository
     * <p>
     * Auto-detects
     *
     * @param repository
     * @param path
     * @throws IOException
     */
    public static void loadRepository(Repository repository, java.nio.file.Path path) throws IOException {
        if (Files.isRegularFile(path)) {
            // If this is a file, we suppose it is a JAR file
            JavaTasksIntrospection.addJarToRepository(repository, path);
        } else {

            // Find the xpm.json file
            java.nio.file.Path jsonPath = path.resolve(JavaTasksIntrospection.XPM_JSON_FILENAME);

            final GsonBuilder gsonBuilder = new GsonBuilder()
                    .registerTypeAdapterFactory(new XPMTypeAdapterFactory());
            gsonBuilder.registerTypeAdapter(java.nio.file.Path.class, new JsonPathAdapter(path));
            final Gson gson = gsonBuilder.create();

            try (final BufferedReader reader = Files.newBufferedReader(jsonPath)) {
                final Collection<TasksInformation> list = gson.fromJson(reader, new TypeToken<Collection<TasksInformation>>() {
                }.getType());
                for (TasksInformation informations : list) {
                    if (informations instanceof JavaTasksInformation) {
                        loadJava(repository, path, jsonPath, (JavaTasksInformation) informations);
                    } else if (informations instanceof ExternalTasksInformation) {
                        loadExternal(repository, gson, (ExternalTasksInformation) informations);
                    }
                }
            }
        }
    }

    private static void loadJava(Repository repository, Path path, Path jsonPath, JavaTasksInformation informations) throws IOException {
        final JavaTasksInformation javaInformations = informations;
        for (Path jarPath : javaInformations.jars) {
            jarPath = path.resolve(jarPath);
            if (javaInformations.binaries == null) {
                throw new IOException("No binaries in JSON " + jsonPath);
            }
            JavaTasksIntrospection.addJarToRepository(repository, jarPath, new JavaCommandLauncher(javaInformations.binaries));
        }
    }

    private static void loadExternal(Repository repository, Gson gson, ExternalTasksInformation informations) throws IOException {
        final ExternalTasksInformation externalTasksInformation = informations;
        for (Map.Entry<Path, Path> entry : externalTasksInformation.tasks_file.entrySet()) {
            Path pyJsonPath = entry.getKey();
            Path pyPath = entry.getValue();

            final ScriptCommandBuilder scriptCommandBuilder = new ScriptCommandBuilder(pyPath);
            try (final BufferedReader pyReader = Files.newBufferedReader(pyJsonPath)) {
                final ArrayList<ScriptTaskInformation> taskList = gson.fromJson(pyReader, Analyze.SCRIPT_INFORMATION_TYPE);
                for (ScriptTaskInformation information : taskList) {
                    // Creates the task factory
                    ScriptTaskFactory factory = new ScriptTaskFactory(repository, information, scriptCommandBuilder, pyPath);
                    repository.addFactory(factory);
                }
            }

        }
    }
}
