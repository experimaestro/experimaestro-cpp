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

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;
import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.connectors.Launcher;
import sf.net.experimaestro.connectors.LocalhostConnector;
import sf.net.experimaestro.connectors.SingleHostConnector;
import sf.net.experimaestro.exceptions.ExitException;
import sf.net.experimaestro.exceptions.ExperimaestroCannotOverwrite;
import sf.net.experimaestro.exceptions.ExperimaestroException;
import sf.net.experimaestro.exceptions.XPMRhinoException;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.experiments.Experiment;
import sf.net.experimaestro.manager.java.JavaTasksIntrospection;
import sf.net.experimaestro.manager.js.*;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonArray;
import sf.net.experimaestro.manager.json.JsonObject;
import sf.net.experimaestro.manager.json.JsonString;
import sf.net.experimaestro.scheduler.Dependency;
import sf.net.experimaestro.scheduler.Resource;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.scheduler.Transaction;
import sf.net.experimaestro.utils.JSUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static sf.net.experimaestro.utils.JSUtils.unwrap;

/**
 * General functions available to all scripting languages
 */
@Exposed
public class Functions {


    @Expose(optional = 1)
    @Help("Defines a new relationship between a network share and a path on a connector")
    public void define_share(@Argument(name = "host", help = "The logical host")
                             String host,
                             @Argument(name = "share")
                             String share,
                             @Argument(name = "connector")
                             SingleHostConnector connector,
                             @Argument(name = "path")
                             String path,
                             @Argument(name = "priority")
                             Integer priority) {
        Scheduler.defineShare(host, share, connector, path, priority == null ? 0 : priority);
    }

    @Expose(optional = 2)
    public void exit(@Argument(name = "code", help = "The exit code") int code,
                     @Argument(name = "message", help = "Formatting template") String message,
                     @Argument(name = "objects", help = "Formatting arguments") Object... objects) {
        if (message == null) throw new ExitException(code);
        if (objects == null) throw new ExitException(code, message);
        throw new ExitException(code, message, objects);
    }

    @Expose
    @Help("Defines the default launcher")
    public void set_default_launcher(Launcher launcher) {
        ScriptContext.threadContext().setDefaultLauncher(launcher);
    }

    @Expose(value = "java_repository", optional = 1, optionalsAtStart = true)
    @Help("Include a repository from introspection of a java project")
    public void includeJavaRepository(Connector connector, String[] paths) throws IOException, ExperimaestroException, ClassNotFoundException {
        if (connector == null)
            connector = LocalhostConnector.getInstance();
        JavaTasksIntrospection.addToRepository(xpm.getRepository(), connector, paths);
    }

    @Expose(scope = true, value = "merge")
    static public NativeObject merge(Context cx, Scriptable scope, Object... objects) {
        NativeObject returned = new NativeObject();

        for (Object object : objects) {
            object = JSUtils.unwrap(object);
            if (object instanceof NativeObject) {
                NativeObject nativeObject = (NativeObject) object;
                for (Map.Entry<Object, Object> entry : nativeObject.entrySet()) {
                    Object key = entry.getKey();
                    if (returned.has(key.toString(), returned))
                        throw new XPMRhinoException("Conflicting id in merge: %s", key);
                    returned.put(key.toString(), returned,
                            JSBaseObject.XPMWrapFactory.INSTANCE.wrap(cx, scope, entry.getValue(), Object.class));
                }
            } else if (object instanceof JsonObject) {
                Json json = (Json) object;
                if (!(json instanceof JsonObject))
                    throw new XPMRhinoException("Cannot merge object of type " + object.getClass());
                JsonObject jsonObject = (JsonObject) json;
                for (Map.Entry<String, Json> entry : jsonObject.entrySet()) {
                    returned.put(entry.getKey(), returned, new JSJson(entry.getValue()));
                }

            } else throw new XPMRhinoException("Cannot merge object of type " + object.getClass());

        }
        return returned;
    }


    @Expose(scope = true)
    public static String digest(Context cx, Scriptable scope, Object... jsons) throws NoSuchAlgorithmException, IOException {
        Json json = JSUtils.toJSON(scope, jsons);
        return Manager.getDigest(json);
    }

    @Expose(scope = true)
    public static String descriptor(Context cx, Scriptable scope, Object... jsons) throws NoSuchAlgorithmException, IOException {
        Json json = JSUtils.toJSON(scope, jsons);
        return Manager.getDescriptor(json);
    }

    @Expose(scope = true)
    @Help(value = "Transform plans outputs with a function")
    public static Scriptable transform(Context cx, Scriptable scope, Callable f, JSAbstractOperator... operators) throws FileSystemException {
        return new JSTransform(cx, scope, f, operators);
    }

    @Expose
    public static JSInput input(String name) {
        return new JSInput(name);
    }

    @Expose(value = "_")
    @JSDeprecated
    public static Object _get_value(Object object) {
        return get_value(object);
    }

    @Expose("$")
    public static Object get_value(Object object) {
        object = unwrap(object);
        if (object instanceof Json)
            return ((Json) object).get();

        return object;
    }

    @Expose("assert")
    public static void _assert(boolean condition, String format, Object... objects) {
        if (!condition)
            throw new EvaluatorException("assertion failed: " + String.format(format, objects));
    }

    @Expose()
    @Help("Get a lock over all the resources defined in a JSON object. When a resource is found, don't try " +
            "to lock the resources below")
    public NativeArray get_locks(String lockMode, JsonObject json) {
        ArrayList<Dependency> dependencies = new ArrayList<>();

        get_locks(lockMode, json, dependencies);

        return new NativeArray(dependencies.toArray(new Dependency[dependencies.size()]));
    }

    private void get_locks(String lockMode, Json json, ArrayList<Dependency> dependencies) {
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
    public Resource get_resource(Json json) {
        Resource resource;
        if (json instanceof JsonObject) {
            resource = getResource((JsonObject) json);
        } else {
            throw new XPMRhinoException("Cannot get the resource of a Json of type " + json.getClass());
        }

        if (resource != null) {
            return resource;
        }
        throw new XPMRhinoException("Object does not contain a resource (key %s)", Manager.XP_RESOURCE);
    }

    private Resource getResource(JsonObject json) {
        if (json.containsKey(Manager.XP_RESOURCE.toString())) {
            final Object o = json.get(Manager.XP_RESOURCE.toString()).get();
            if (o instanceof Resource) {
                return (Resource) o;
            } else {
                final String uri = o instanceof JsonString ? o.toString() : (String) o;
                if (xpm.simulate()) {
                    final Resource resource = xpm.submittedJobs.get(uri);
                    if (resource == null) {
                        return Transaction.evaluate(em -> Resource.getByLocator(em, uri));
                    }
                    return resource;
                } else {
                    return Transaction.evaluate(em -> Resource.getByLocator(em, uri));
                }
            }

        }
        return null;
    }

    @Expose()
    @Help("Set the experiment for all future commands")
    public void set_experiment(String dotname, java.nio.file.Path workdir) throws ExperimaestroCannotOverwrite {
        if (!xpm.simulate()) {
            Experiment experiment = new Experiment(dotname, System.currentTimeMillis(), workdir);
            try (Transaction t = Transaction.create()) {
                t.em().persist(experiment);
                xpm.getScriptContext().setExperimentId(experiment.getId());
                t.commit();
            }
        }
        xpm.getScriptContext().setWorkingDirectory(workdir);
    }

    @Expose
    public void set_workdir(java.nio.file.Path workdir) throws FileSystemException {
        xpm.getScriptContext().setWorkingDirectory(workdir);
    }

    /**
     * Includes a repository
     *
     * @param _connector
     * @param path
     * @param repositoryMode True if we include a repository
     * @return
     */
    public XPMObject include(Object _connector, String path, boolean repositoryMode)
            throws Exception {
        // Get the connector
        if (_connector instanceof Wrapper)
            _connector = ((Wrapper) _connector).unwrap();

        Connector connector;
        if (_connector instanceof JSConnector)
            connector = ((JSConnector) _connector).getConnector();
        else
            connector = (Connector) _connector;

        return include(connector.resolve(path), repositoryMode);
    }

    /**
     * Includes a repository
     *
     * @param path           The xpath, absolute or relative to the current evaluated script
     * @param repositoryMode If true, creates a new javascript scope that will be independant of this one
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public XPMObject include(String path, boolean repositoryMode) throws Exception {
        java.nio.file.Path scriptpath = currentScriptPath.getParent().resolve(path);
        LOGGER.debug("Including repository file [%s]", scriptpath);
        return include(scriptpath, repositoryMode);
    }

    /**
     * Central method called for any script inclusion
     *
     * @param scriptPath     The path to the script
     * @param repositoryMode If true, runs in a separate environement
     * @throws Exception if something goes wrong
     */

    private XPMObject include(java.nio.file.Path scriptPath, boolean repositoryMode) throws Exception {

        java.nio.file.Path oldResourceLocator = currentScriptPath;
        try (InputStream inputStream = Files.newInputStream(scriptPath)) {
            Scriptable scriptScope = scope;
            XPMObject xpmObject = this;
            currentScriptPath = scriptPath;

            if (repositoryMode) {
                // Run the script in a new environment
                scriptScope = JavascriptContext.newScope();
                final TreeMap<String, String> newEnvironment = new TreeMap<>(environment);
                xpmObject = clone(scriptPath, scriptScope, newEnvironment);
                threadXPM.set(xpmObject);
            }

            // Avoid adding the protocol if this is a local file
            final String sourceName = scriptPath.toString();


            Context.getCurrentContext().evaluateReader(scriptScope, new InputStreamReader(inputStream), sourceName, 1, null);

            return xpmObject;
        } catch (FileNotFoundException e) {
            throw new XPMRhinoException("File not found: %s", scriptPath);
        } finally {
            threadXPM.set(this);
            currentScriptPath = oldResourceLocator;
        }

    }
    /**
     * Javascript constructor calling {@linkplain #include(String, boolean)}
     */
    @Expose
    static public void include(Context cx, Scriptable thisObj, Object[] args,
                                  Function funObj) throws Exception {

        include(cx, thisObj, args, funObj, false);
    }


    /**
     * Returns a JSPath that corresponds to the path. This can
     * be used when building command lines containing path to resources
     * or executables
     *
     * @return A {@JSPath}
     */
    @Help("Returns a Path corresponding to the path")
    static public Object js_path(Context cx, Scriptable thisObj, Object[] args, Function funObj) throws FileSystemException, URISyntaxException {
        if (args.length != 1)
            throw new IllegalArgumentException("path() needs one argument");

        XPMObject xpm = getXPM(thisObj);

        if (args[0] instanceof ScriptingPath)
            return args[0];

        final Object o = unwrap(args[0]);

        if (o instanceof ScriptingPath)
            return o;

        if (o instanceof java.nio.file.Path)
            return xpm.newObject(ScriptingPath.class, o);

        if (o instanceof String) {
            final java.nio.file.Path path = Paths.get(new URI(o.toString()));
            if (!path.isAbsolute()) {
                return xpm.newObject(ScriptingPath.class, xpm.currentScriptPath.getParent().resolve(path));
            }
            return path;
        }

        throw new XPMRuntimeException("Cannot convert type [%s] to a file xpath", o.getClass().toString());
    }

    @Expose
    static public String format(
            @Argument(name = "format", type = "String", help = "The string used to format") String format,
            @Argument(name = "arguments...", type = "Object", help = "A list of objects")
            Object[] args) {
        if (args.length == 0)
            return "";

        Object fargs[] = new Object[args.length - 1];
        for (int i = 1; i < args.length; i++)
            fargs[i - 1] = unwrap(args[i]);
        return String.format(format, fargs);
    }

    @Expose()
    @Help("Returns the Path object corresponding to the current script")
    static public java.nio.file.Path script_file()
            throws FileSystemException {
        return ScriptContext.threadContext().getCurrentScriptPath();
    }

    @Help(value = "Returns a file relative to the current connector")
    public static Scriptable js_file(Context cx, Scriptable thisObj, Object[] args, Function funObj) throws FileSystemException {
        XPMObject xpm = getXPM(thisObj);
        if (args.length != 1)
            throw new IllegalArgumentException("file() takes only one argument");
        final String arg = JSUtils.toString(args[0]);
        return xpm.context.newObject(xpm.scope, ScriptingPath.JSCLASSNAME,
                new Object[]{xpm.currentScriptPath.getParent().resolve(arg)});
    }
    /**
     * Recursive flattening of an array
     *
     * @param array The array to flatten
     * @param list  A list of strings that will be filled
     */
    static public void flattenArray(NativeArray array, List<String> list) {
        int length = (int) array.getLength();

        for (int i = 0; i < length; i++) {
            Object el = array.get(i, array);
            if (el instanceof NativeArray) {
                flattenArray((NativeArray) el, list);
            } else
                list.add(toString(el));
        }

    }

}
