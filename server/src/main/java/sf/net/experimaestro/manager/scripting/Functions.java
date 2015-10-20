package sf.net.experimaestro.manager.scripting;

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

import org.apache.log4j.Hierarchy;
import org.mozilla.javascript.*;
import sf.net.experimaestro.connectors.*;
import sf.net.experimaestro.exceptions.*;
import sf.net.experimaestro.manager.Constants;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.experiments.Experiment;
import sf.net.experimaestro.manager.java.JavaTasksIntrospection;
import sf.net.experimaestro.manager.js.JSBaseObject;
import sf.net.experimaestro.manager.js.JSTransform;
import sf.net.experimaestro.manager.js.JavaScriptContext;
import sf.net.experimaestro.manager.js.JavaScriptRunner;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonArray;
import sf.net.experimaestro.manager.json.JsonObject;
import sf.net.experimaestro.manager.json.JsonString;
import sf.net.experimaestro.manager.plans.FunctionOperator;
import sf.net.experimaestro.manager.plans.Operator;
import sf.net.experimaestro.manager.plans.ProductReference;
import sf.net.experimaestro.scheduler.Dependency;
import sf.net.experimaestro.scheduler.Resource;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.log.Logger;

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
import java.util.Map;

import static sf.net.experimaestro.utils.JSUtils.unwrap;

/**
 * General functions available to all scripting languages
 */
@Exposed
public class Functions {
    final static private Logger LOGGER = Logger.getLogger();

    @Expose(context = true, value = "merge")
    static public NativeObject merge(LanguageContext cx,
                                     @Argument(name = "objects", types = {Map.class, Json.class})
                                     Object... objects) {
        NativeObject returned = new NativeObject();

        Scriptable scope = null; // FIXME
        Context jcx = null;

        for (Object object : objects) {
            object = JSUtils.unwrap(object);
            if (object instanceof Map) {
                Map<?, ?> map = (Map) object;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    Object key = entry.getKey();
                    if (returned.has(key.toString(), returned))
                        throw new XPMRhinoException("Conflicting id in merge: %s", key);
                    returned.put(key.toString(), returned,
                            JSBaseObject.XPMWrapFactory.INSTANCE.wrap(jcx, scope, entry.getValue(), Object.class));
                }
            } else if (object instanceof Json) {
                Json json = (Json) object;
                if (!(json instanceof JsonObject))
                    throw new XPMRhinoException("Cannot merge object of type " + object.getClass());
                JsonObject jsonObject = (JsonObject) json;
                for (Map.Entry<String, Json> entry : jsonObject.entrySet()) {
                    returned.put(entry.getKey(), returned, entry.getValue());
                }

            } else throw new XPMRhinoException("Cannot merge object of type " + object.getClass());

        }
        return returned;
    }

    @Expose(context = true)
    public static String digest(LanguageContext cx, Object... jsons) throws NoSuchAlgorithmException, IOException {
        Json json = cx.toJSON(jsons);
        return Manager.getDigest(json);
    }

    @Expose(context = true)
    public static String descriptor(LanguageContext cx, Object... jsons) throws NoSuchAlgorithmException, IOException {
        Json json = cx.toJSON(jsons);
        return Manager.getDescriptor(json);
    }

    @Expose(context = true)
    @Help(value = "Transform plans outputs with a function")
    public static Operator transform(JavaScriptContext cx, @NoJavaization Callable f, Operator... operators) throws FileSystemException {
        final JSTransform transform = new JSTransform(cx, f, operators);
        FunctionOperator transformOperator = new FunctionOperator(ScriptContext.get(), transform);

        Operator inputOperator;
        if (operators.length == 1)
            inputOperator = operators[0];
        else {
            ProductReference pr = new ProductReference(ScriptContext.get());
            for (Operator operator : operators) {
                pr.addParent(operator);
            }
            inputOperator = pr;
        }
        transformOperator.addParent(inputOperator);

        return transformOperator;
    }

    // Used in scripting languages which cannot use $ as first character
    @Expose(value = "_", languages = Languages.PYTHON)
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
        return new QName(ns, localName);
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
            sc.setCurrentScriptPath(scriptPath);

            Scriptable scope;
            final String sourceName = scriptPath.toString();

            if (cx instanceof JavaScriptContext) {
                scope = ((JavaScriptContext) cx).scope();

                final Object result = Context.getCurrentContext().evaluateReader(scope, new InputStreamReader(inputStream), sourceName, 1, null);
                return repositoryMode ? sc.properties : result;
            } else {
                try (JavaScriptRunner jsXPM = new JavaScriptRunner(sc.getRepository(), sc.getScheduler(), (Hierarchy) sc.getMainLogger().getLoggerRepository(), null, sc)) {
                    final Object result = jsXPM.evaluateReader(new InputStreamReader(inputStream), sourceName, 1, null);
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

    @Expose(value="to_json", context=true)
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
    @Help("Include a repository from introspection of a JAR file")
    static public void includeJavaRepository(Path jarPath) throws IOException, ExperimaestroException, ClassNotFoundException {
        JavaTasksIntrospection.addJarToRepository(context().getRepository(), jarPath);
    }

    @Expose()
    @Help("Get a lock over all the resources defined in a JSON object. When a resource is found, don't try " +
            "to lock the resources below")
    static public ArrayList<Dependency> get_locks(String lockMode, JsonObject json) throws SQLException {
        ArrayList<Dependency> dependencies = new ArrayList<>();

        get_locks(lockMode, json, dependencies);

        return dependencies;
    }

    static private void get_locks(String lockMode, Json json, ArrayList<Dependency> dependencies) throws SQLException {
        if (json instanceof JsonObject) {
            final Resource resource = getResource((JsonObject) json);
            if (resource != null) {
                final Dependency dependency = resource.createDependency(lockMode);
                dependencies.add(dependency);
            } else {
                for (Json element : ((JsonObject) json).values()) {
                    get_locks(lockMode, element, dependencies);
                }

            }
        } else if (json instanceof JsonArray) {
            for (Json arrayElement : ((JsonArray) json)) {
                get_locks(lockMode, arrayElement, dependencies);
            }

        }
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
                if (RunningContext.get().simulate()) {
                    final Resource resource = RunningContext.get().getSubmittedJobs().get(uri);
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

    @Expose()
    @Help("Set the experiment for all future commands")
    static public void  set_experiment(String dotname) throws ExperimaestroCannotOverwrite, SQLException {
        if (!RunningContext.get().simulate()) {
            Experiment experiment = new Experiment(dotname, System.currentTimeMillis());
            experiment.save();
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
}
