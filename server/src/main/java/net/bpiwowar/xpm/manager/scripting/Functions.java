package net.bpiwowar.xpm.manager.scripting;

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

import net.bpiwowar.xpm.connectors.Connector;
import net.bpiwowar.xpm.connectors.Launcher;
import net.bpiwowar.xpm.connectors.LocalhostConnector;
import net.bpiwowar.xpm.connectors.NetworkShare;
import net.bpiwowar.xpm.connectors.SingleHostConnector;
import net.bpiwowar.xpm.exceptions.ExitException;
import net.bpiwowar.xpm.exceptions.ExperimaestroCannotOverwrite;
import net.bpiwowar.xpm.exceptions.ExperimaestroException;
import net.bpiwowar.xpm.exceptions.XPMRhinoException;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.exceptions.XPMScriptRuntimeException;
import net.bpiwowar.xpm.manager.Constants;
import net.bpiwowar.xpm.manager.JsonSignature;
import net.bpiwowar.xpm.manager.TypeName;
import net.bpiwowar.xpm.manager.experiments.Experiment;
import net.bpiwowar.xpm.manager.js.JavaScriptContext;
import net.bpiwowar.xpm.manager.js.JavaScriptRunner;
import net.bpiwowar.xpm.manager.json.Json;
import net.bpiwowar.xpm.manager.json.JsonArray;
import net.bpiwowar.xpm.manager.json.JsonObject;
import net.bpiwowar.xpm.manager.json.JsonString;
import net.bpiwowar.xpm.manager.tasks.JavaTasksIntrospection;
import net.bpiwowar.xpm.manager.tasks.TasksLoader;
import net.bpiwowar.xpm.scheduler.Dependency;
import net.bpiwowar.xpm.scheduler.DependencyParameters;
import net.bpiwowar.xpm.scheduler.Resource;
import net.bpiwowar.xpm.scheduler.ResourceState;
import net.bpiwowar.xpm.scheduler.Scheduler;
import net.bpiwowar.xpm.utils.log.Logger;
import org.apache.log4j.Hierarchy;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Scriptable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import static net.bpiwowar.xpm.utils.JSUtils.unwrap;

/**
 * General functions available to all scripting languages
 */
@Exposed
public class Functions {
    final static private Logger LOGGER = Logger.getLogger();

    @Expose(context = true, value = "merge")
    static public Map merge(LanguageContext cx,
                            @Argument(name = "objects", types = {Map.class, Json.class})
                            Object... objects) {
        Map<String, Object> returned = new HashMap<>();

        for (Object object : objects) {
            if (object instanceof Map) {
                Map<?, ?> map = (Map) object;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    Object key = entry.getKey();
                    if (returned.containsKey(key.toString()))
                        throw new XPMRhinoException("Conflicting id in merge: %s", key);
                    returned.put(key.toString(), entry.getValue());
                }
            } else if (object instanceof Json) {
                Json json = (Json) object;
                if (!(json instanceof JsonObject))
                    throw new XPMRhinoException("Cannot merge object of type " + object.getClass());
                JsonObject jsonObject = (JsonObject) json;
                for (Map.Entry<String, Json> entry : jsonObject.entrySet()) {
                    returned.put(entry.getKey(), entry.getValue());
                }

            } else throw new XPMRhinoException("Cannot merge object of type " + object.getClass());

        }
        return returned;
    }

    @Expose(context = true)
    public static String digest(LanguageContext cx, Object... jsons) throws NoSuchAlgorithmException, IOException {
        Json json = cx.toJSON(jsons);
        return JsonSignature.getDigest(json);
    }

    @Expose(context = true)
    public static String descriptor(LanguageContext cx, Object... jsons) throws NoSuchAlgorithmException, IOException {
        Json json = cx.toJSON(jsons);
        return JsonSignature.getDescriptor(json);
    }

    // Used in scripting languages which cannot use $ as first character
    @Expose(value = "v_", languages = Languages.PYTHON)
    public static Object _get_value(Object object) {
        return get_value(object);
    }

    @Expose(value = "$", languages = Languages.JAVASCRIPT)
    public static Object get_value(Object object) {
        object = unwrap(object);
        if (object instanceof Json)
            return ((Json) object).get();

        return object;
    }

    @Expose("assert")
    public static void _assert(boolean condition, String format, Object... objects) {
        if (!condition) {
            throw new EvaluatorException("assertion failed: " + String.format(format, objects));
        }
    }

    private static ScriptContext context() {
        return ScriptContext.get();
    }

    /**
     * Returns a QName object
     *
     * @param ns        The namespace: can be the URI string, or a javascript
     *                  Namespace object
     * @param localName the localname
     * @return a QName object
     */
    @Expose
    static public Object qname(String ns, String localName) {
        return new TypeName(ns, localName);
    }

    @Expose(context = true)
    static public Map<String, Object> include_repository(LanguageContext cx, Path path) throws Exception {
        return (Map<String, Object>) include(cx, path, true);
    }

    @Expose(context = true)
    static public Map<String, Object> include_repository(LanguageContext cx, String path) throws Exception {
        final URI _uri = new URI(path);
        final Path scriptPath;
        if (_uri.getScheme() == null) {
            scriptPath = context().get().getCurrentScriptPath().getParent().resolve(path);
        } else {
            scriptPath = Paths.get(_uri);
        }
        return (Map<String, Object>) include(cx, scriptPath, true);
    }

    /**
     * Central method called for any script inclusion
     *
     * @param scriptPath     The path to the script
     * @param repositoryMode If true, runs in a separate environement
     * @throws Exception if something goes wrong
     */

    private static Object include(LanguageContext cx, java.nio.file.Path scriptPath, boolean repositoryMode) throws Exception {
        try (InputStream inputStream = Files.newInputStream(scriptPath); ScriptContext sc = context().copy(repositoryMode, true)) {

            Scriptable scope;
            final String sourceName = scriptPath.toString();

            if (cx instanceof JavaScriptContext) {
                scope = ((JavaScriptContext) cx).scope();
                sc.setCurrentScriptPath(scriptPath);
                final Object result = Context.getCurrentContext().evaluateReader(scope, new InputStreamReader(inputStream), sourceName, 1, null);
                return repositoryMode ? sc.properties : result;
            } else {
                try (JavaScriptRunner jsXPM = new JavaScriptRunner(sc.getRepository(), sc.getScheduler(), (Hierarchy) sc.getMainLogger().getLoggerRepository(), null, sc)) {
                    final Object result = jsXPM.evaluateReader(new InputStreamReader(inputStream), scriptPath, 1, null);
                    return repositoryMode ? sc.properties : result;
                }
            }
        } catch (FileNotFoundException e) {
            throw new XPMRhinoException("File not found: %s", scriptPath);
        }
    }

    @Expose
    static public String format(
            @Argument(name = "format", type = "String", help = "The string used to format")
            String format,
            @Argument(name = "arguments...", type = "Object", help = "A list of objects")
            Object... args) {
        return String.format(format, args);
    }

    @Expose()
    @Help("Returns the Path object corresponding to the current script")
    static public java.nio.file.Path script_file()
            throws FileSystemException {
        return ScriptContext.get().getCurrentScriptPath();
    }

    @Expose()
    static public java.nio.file.Path path(@Argument(name = "uri") Path path) {
        return path;
    }

    @Expose()
    @Help("Returns a path object from an URI")
    static public java.nio.file.Path path(@Argument(name = "uri") String uri)
            throws FileSystemException, URISyntaxException {
        final URI _uri = new URI(uri);
        return _uri.getScheme() == null ? Paths.get(uri) : Paths.get(_uri);
    }


    @Expose(optional = 1)
    @Help("Defines a new relationship between a network share and a path on a connector")
    static public void define_share(@Argument(name = "host", help = "The logical host")
                                    String host,
                                    @Argument(name = "share")
                                    String share,
                                    @Argument(name = "connector")
                                    SingleHostConnector connector,
                                    @Argument(name = "path")
                                    String path,
                                    @Argument(name = "priority")
                                    Integer priority) throws SQLException {
        Scheduler.defineShare(host, share, connector, path, priority == null ? 0 : priority);
    }

    @Expose(optional = 2)
    static public void exit(@Argument(name = "code", help = "The exit code") int code,
                            @Argument(name = "message", help = "Formatting template") String message,
                            @Argument(name = "objects", help = "Formatting arguments") Object... objects) {
        if (message == null) throw new ExitException(code);
        if (objects == null) throw new ExitException(code, message);
        throw new ExitException(code, message, objects);
    }

    @Expose(value = "to_json", context = true)
    static public Json toJson(LanguageContext lcx, Object o) {
        return lcx.toJSON(o);
    }

    @Expose
    @Help("Defines the default launcher")
    static public void set_default_launcher(Launcher launcher) {
        ScriptContext.get().setDefaultLauncher(launcher);
    }

    @Expose(value = "inspect_java_repository", optional = 1)
    @Help("Include a repository from introspection of a java project")
    static public void includeJavaRepository(final Connector connector, String[] paths, Path cachePath) throws IOException, ExperimaestroException, ClassNotFoundException {
        Path[] classpath = Arrays.stream(paths).map(path -> {
            try {
                return connector.getMainConnector().resolveFile(path);
            } catch (IOException e) {
                throw new XPMRuntimeException(e, "Could not resolve path %s", path);
            }
        }).toArray(n -> new Path[n]);

        JavaTasksIntrospection.addToRepository(context().getRepository(), classpath, cachePath);
    }

    @Expose(value = "load_java_repository")
    @Deprecated("Use load_repository")
    @Help("Include a repository from introspection of a JAR file")
    static public void includeJavaRepository(Path jarPath) throws IOException, ExperimaestroException, ClassNotFoundException {
        loadRepository(jarPath);
    }

    @Expose(value = "load_repository")
    @Help("Include a repository from introspection of a JAR file")
    static public void loadRepository(Path jarPath) throws IOException, ExperimaestroException, ClassNotFoundException {
        TasksLoader.loadRepository(context().getRepository(), jarPath);
    }

    @Deprecated("Use get_locks(json, parameters...)")
    static public ArrayList<Dependency> get_locks(String lockMode, JsonObject json) throws SQLException {
        switch (lockMode) {
            case "READ":
                return get_locks(json);
        }
        throw new XPMScriptRuntimeException("Cannot handle %s lock mode");
    }

    @Expose()
    @Help("Get a lock over all the resources defined in a JSON object. When a resource is found, don't try " +
            "to lock the resources below")
    static public ArrayList<Dependency> get_locks(JsonObject json, DependencyParameters... parameters) throws SQLException {
        ArrayList<Dependency> dependencies = new ArrayList<>();

        IdentityHashMap<Object, DependencyParameters> pmap = new IdentityHashMap<>();
        for (DependencyParameters parameter : parameters) {
            pmap.put(parameter.getKey(), parameter);
        }

        get_locks(json, pmap, dependencies);

        return dependencies;
    }

    static private void get_locks(Json json, IdentityHashMap<Object, DependencyParameters> pmap, ArrayList<Dependency> dependencies) throws SQLException {
        if (json instanceof JsonObject) {
            final Resource resource = getResource((JsonObject) json);
            if (resource != null) {
                final Dependency dependency = resource.createDependency(pmap);
                dependencies.add(dependency);
            } else {
                for (Json element : ((JsonObject) json).values()) {
                    get_locks(element, pmap, dependencies);
                }

            }
        } else if (json instanceof JsonArray) {
            for (Json arrayElement : ((JsonArray) json)) {
                get_locks(arrayElement, pmap, dependencies);
            }

        }
    }

    @Expose(value = "r_", languages = Languages.PYTHON)
    public static Object get_resource_py(Json json) throws SQLException {
        return get_resource(json);
    }

    @Expose(value = "$$")
    @Help("Get the resource associated with the json object")
    static public Resource get_resource(Json json) throws SQLException {
        Resource resource;
        if (json instanceof JsonObject) {
            resource = getResource((JsonObject) json);
        } else {
            throw new XPMRhinoException("Cannot get the resource of a Json of type " + json.getClass());
        }

        if (resource != null) {
            return resource;
        }
        throw new XPMRhinoException("Object does not contain a resource (key %s)", Constants.XP_RESOURCE);
    }

    private static Resource getResource(JsonObject json) throws SQLException {
        if (json.containsKey(Constants.XP_RESOURCE.toString())) {
            final Object o = json.get(Constants.XP_RESOURCE.toString()).get();
            if (o instanceof Resource) {
                return (Resource) o;
            } else {
                final String uri = o instanceof JsonString ? o.toString() : (String) o;
                Path path = NetworkShare.uriToPath(uri);
                if (ScriptContext.get().simulate()) {
                    final Resource resource = ScriptContext.get().getSubmittedJobs().get(uri).resource;
                    if (resource == null) {
                        return Resource.getByLocator(path);
                    }
                    return resource;
                } else {
                    return Resource.getByLocator(path);
                }
            }

        }
        return null;
    }

    @Expose(optional = 1)
    @Help("Set the experiment for all future commands")
    static public void set_experiment(
            @Argument(name = "identifier", help = "Name of the experiment") String identifier,
            @Argument(name = "holdPrevious") Boolean holdPrevious
    ) throws ExperimaestroCannotOverwrite, SQLException {
        final ScriptContext scriptContext = ScriptContext.get();
        if (!scriptContext.simulate()) {
            // We first put on hold all the resources belonging to this experiment
            if (holdPrevious == null || holdPrevious) {
                for (Resource resource : Experiment.resourcesByIdentifier(identifier, ResourceState.WAITING_STATES)) {
                    synchronized (resource.getState()) {
                        if (resource.getState().isWaiting()) {
                            resource.setState(ResourceState.ON_HOLD);
                        }
                    }
                }
            }


            Experiment experiment = new Experiment(identifier, System.currentTimeMillis());
            experiment.save();
            scriptContext.setExperiment(experiment);
        }
    }

    @Expose
    static public void set_workdir(java.nio.file.Path workdir) throws FileSystemException {
        context().setWorkingDirectory(workdir);
    }

    @Expose(context = true)
    static public Object include(LanguageContext cx, String path)
            throws Exception {
        return include(cx, ScriptContext.get().getCurrentScriptPath().getParent().resolve(path), false);
    }

    /**
     * Includes a repository
     *
     * @param connector
     * @param path
     * @param repositoryMode True if we include a repository
     * @return
     */
    public Object include(LanguageContext cx, Connector connector, String path, boolean repositoryMode)
            throws Exception {
        return include(cx, connector.resolve(path), repositoryMode);
    }

    /**
     * Includes a repository
     *
     * @param cx             The language context
     * @param path           The xpath, absolute or relative to the current evaluated script
     * @param repositoryMode If true, creates a new javascript scope that will be independant of this one
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public Object include(LanguageContext cx, String path, boolean repositoryMode) throws Exception {
        java.nio.file.Path scriptpath = context().getCurrentScriptPath().getParent().resolve(path);
        LOGGER.debug("Including repository file [%s]", scriptpath);
        return include(cx, scriptpath, repositoryMode);
    }

    @Expose
    static public LocalhostConnector get_localhost_connector() {
        return Scheduler.get().getLocalhostConnector();
    }

    @Expose
    static public String parameters(String key) {
        return ScriptContext.get().getParameter(key);
    }

    @Expose(context = true)
    @Help("Annotate a value with a tag, marked by " + Constants.JSON_TAG_NAME + " in the JSON")
    static public Json tag(LanguageContext cx, String tagvalue, Object element) {
        final JsonObject jsonObject = toJson(cx, element).asObject();
        jsonObject.put(Constants.JSON_TAG_NAME, tagvalue);
        return jsonObject;
    }

    @Expose
    @Help("Find all tags and return a hash map")
    static public Map<String, Object> find_tags(Json json) {
        HashMap<String, Object> tags = new HashMap<>();
        find_tags(json, tags);
        return tags;
    }

    @Expose(context = true, optional = 1, optionalsAtStart = true)
    @Help("Find all tags and add it to the base object")
    static public Json retrieve_tags(
            LanguageContext cx,
            @Argument(name = "key", help = "The key in the returned JSON")
            String key,
            @Argument(name = "json", help = "The JSON to inspect")
            JsonObject json
    ) {
        final Map<String, Object> tags = find_tags(json);
        if (json.sealed()) {
            json = (JsonObject) json.copy(false);
        }
        json.put(key != null ? key : Constants.JSON_TAGS_NAME, Json.toJSON(cx, tags));
        return json;
    }

    private static void find_tags(Json json, HashMap<String, Object> tags) {
        if (json.is_object()) {
            final JsonObject object = (JsonObject) json;
            if (object.containsKey(Constants.JSON_TAG_NAME)) {
                tags.put(object.get(Constants.JSON_TAG_NAME).get().toString(), object.get());
            }

            for (Json v : object.values()) {
                find_tags(v, tags);
            }

        } else if (json.is_array()) {
            for (Json v : ((JsonArray) json)) {
                find_tags(v, tags);
            }
        }
    }

    @Expose
    @Help("Returns the notification URL")
    static public String notification_url() {
        return Scheduler.get().getURL();
    }

}
