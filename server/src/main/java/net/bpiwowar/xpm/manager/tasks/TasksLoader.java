package net.bpiwowar.xpm.manager.tasks;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.exceptions.XPMScriptRuntimeException;
import net.bpiwowar.xpm.manager.Repository;
import net.bpiwowar.xpm.utils.gson.JsonPathAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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

            final GsonBuilder gsonBuilder =  new GsonBuilder()
                    .registerTypeAdapterFactory(new XPMTypeAdapterFactory());
            gsonBuilder.registerTypeAdapter(java.nio.file.Path.class, new JsonPathAdapter(path));
            gsonBuilder.registerTypeAdapter(CommandArgument.class, new CommandArgument.TypeAdapter());
            final Gson gson = gsonBuilder.create();

            try (final BufferedReader reader = Files.newBufferedReader(jsonPath)) {
                final Collection<TasksInformation> list = gson.fromJson(reader, new TypeToken<Collection<TasksInformation>>() {
                }.getType());
                for (TasksInformation informations : list) {
                    if (informations instanceof JavaTasksInformation) {
                        loadJava(repository, path, jsonPath, (JavaTasksInformation) informations);
                    } else if (informations instanceof ScriptsTaskInformation) {
                        loadExternal(repository, gson, (ScriptsTaskInformation) informations);
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

    /**
     * Load scripts task information
     *
     * @param repository   The repository
     * @param gson         The gson
     * @param informations
     * @throws IOException
     */
    private static void loadExternal(Repository repository, Gson gson, ScriptsTaskInformation informations) throws IOException {
        final ScriptCommandBuilder scriptCommandBuilder = new ScriptCommandBuilder(informations);
        Map<String, String> prefixes = new HashMap<>();
        informations.namespaces.forEach((k, v) -> prefixes.put(v, k));

        for (Map.Entry<Path, Path> entry : informations.tasks_file.entrySet()) {
            Path scriptJsonPath = entry.getKey();
            Path scriptPath = entry.getValue();

            try (final BufferedReader pyReader = Files.newBufferedReader(scriptJsonPath)) {
                try {
                    final ArrayList<ScriptTaskInformation> taskList = gson.fromJson(pyReader, Analyze.SCRIPT_INFORMATION_TYPE);
                    for (ScriptTaskInformation information : taskList) {
                        // Creates the task factory
                        information.prefixes = prefixes;
                        ScriptTaskFactory factory = new ScriptTaskFactory(repository, information, scriptCommandBuilder, scriptPath);
                        repository.addFactory(factory);
                    }
                } catch(RuntimeException e) {
                    final XPMScriptRuntimeException e2 = new XPMScriptRuntimeException(e, "Error while reading " + scriptJsonPath);
                    e2.addContext("while reading %s", scriptJsonPath);
                    throw e2;
                }
            }

        }
    }
}
