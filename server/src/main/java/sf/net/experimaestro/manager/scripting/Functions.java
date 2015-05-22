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

import org.mozilla.javascript.*;
import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.connectors.Launcher;
import sf.net.experimaestro.connectors.LocalhostConnector;
import sf.net.experimaestro.connectors.SingleHostConnector;
import sf.net.experimaestro.exceptions.ExitException;
import sf.net.experimaestro.exceptions.ExperimaestroCannotOverwrite;
import sf.net.experimaestro.exceptions.ExperimaestroException;
import sf.net.experimaestro.exceptions.XPMRhinoException;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.experiments.Experiment;
import sf.net.experimaestro.manager.java.JavaTasksIntrospection;
import sf.net.experimaestro.manager.js.JSBaseObject;
import sf.net.experimaestro.manager.js.JSTransform;
import sf.net.experimaestro.manager.js.JavaScriptContext;
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
import sf.net.experimaestro.scheduler.Transaction;
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
import java.util.ArrayList;
import java.util.Map;

import static sf.net.experimaestro.utils.JSUtils.unwrap;

/**
 * General functions available to all scripting languages
 */
@Exposed
public class Functions {
    final static private Logger LOGGER = Logger.getLogger();

    @Expose(context = true, value = "merge")
    static public NativeObject merge(LanguageContext cx, Object... objects) {
        NativeObject returned = new NativeObject();

        Scriptable scope = null; // FIXME
        Context jcx = null;

        for (Object object : objects) {
            object = JSUtils.unwrap(object);
            if (object instanceof NativeObject) {
                NativeObject nativeObject = (NativeObject) object;
                for (Map.Entry<Object, Object> entry : nativeObject.entrySet()) {
                    Object key = entry.getKey();
                    if (returned.has(key.toString(), returned))
                        throw new XPMRhinoException("Conflicting id in merge: %s", key);
                    returned.put(key.toString(), returned,
                            JSBaseObject.XPMWrapFactory.INSTANCE.wrap(jcx, scope, entry.getValue(), Object.class));
                }
            } else if (object instanceof JsonObject) {
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
        FunctionOperator transformOperator = new FunctionOperator(transform);

        Operator inputOperator;
        if (operators.length == 1)
            inputOperator = operators[0];
        else {
            ProductReference pr = new ProductReference();
            for (Operator operator : operators) {
                pr.addParent(operator);
            }
            inputOperator = pr;
        }
        transformOperator.addParent(inputOperator);

        return transformOperator;
    }

    @Expose(value = "_")
    @Deprecated
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
        final Path scriptPath = context().get().getCurrentScriptPath().getParent().resolve(path);
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
        java.nio.file.Path oldResourceLocator = context().getCurrentScriptPath();
        try (InputStream inputStream = Files.newInputStream(scriptPath); ScriptContext sc = context().copy(repositoryMode)) {
            sc.setCurrentScriptPath(scriptPath);
            Scriptable scope = ((JavaScriptContext) cx).scope();
            // Avoid adding the protocol if this is a local file
            final String sourceName = scriptPath.toString();

            final Object result = Context.getCurrentContext().evaluateReader(scope, new InputStreamReader(inputStream), sourceName, 1, null);
            return repositoryMode ? sc.properties : result;
        } catch (FileNotFoundException e) {
            throw new XPMRhinoException("File not found: %s", scriptPath);
        }
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
        return ScriptContext.get().getCurrentScriptPath();
    }

    @Expose()
    @Help("Returns a path object from an URI")
    static public java.nio.file.Path path(@Argument(name = "uri") String uri)
            throws FileSystemException, URISyntaxException {
        return Paths.get(new URI(uri));
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
                                    Integer priority) {
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

    @Expose
    @Help("Defines the default launcher")
    static public void set_default_launcher(Launcher launcher) {
        ScriptContext.get().setDefaultLauncher(launcher);
    }

    @Expose(value = "java_repository", optional = 1, optionalsAtStart = true)
    @Help("Include a repository from introspection of a java project")
    static public void includeJavaRepository(Connector connector, String[] paths) throws IOException, ExperimaestroException, ClassNotFoundException {
        if (connector == null)
            connector = LocalhostConnector.getInstance();
        JavaTasksIntrospection.addToRepository(context().getRepository(), connector, paths);
    }

    @Expose()
    @Help("Get a lock over all the resources defined in a JSON object. When a resource is found, don't try " +
            "to lock the resources below")
    static public ArrayList<Dependency> get_locks(String lockMode, JsonObject json) {
        ArrayList<Dependency> dependencies = new ArrayList<>();

        get_locks(lockMode, json, dependencies);

        return dependencies;
    }

    static private void get_locks(String lockMode, Json json, ArrayList<Dependency> dependencies) {
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
    static public Resource get_resource(Json json) {
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

    private static Resource getResource(JsonObject json) {
        if (json.containsKey(Manager.XP_RESOURCE.toString())) {
            final Object o = json.get(Manager.XP_RESOURCE.toString()).get();
            if (o instanceof Resource) {
                return (Resource) o;
            } else {
                final String uri = o instanceof JsonString ? o.toString() : (String) o;
                final ScriptContext scriptContext = ScriptContext.get();
                if (scriptContext.simulate()) {
                    final Resource resource = scriptContext.submittedJobs.get(uri);
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
    static public void set_experiment(String dotname, java.nio.file.Path workdir) throws ExperimaestroCannotOverwrite {
        if (!context().simulate()) {
            Experiment experiment = new Experiment(dotname, System.currentTimeMillis(), workdir);
            try (Transaction t = Transaction.create()) {
                t.em().persist(experiment);
                context().setExperimentId(experiment.getId());
                t.commit();
            }
        }
        context().setWorkingDirectory(workdir);
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
}
