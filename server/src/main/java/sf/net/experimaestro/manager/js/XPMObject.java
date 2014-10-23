/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.manager.js;

import bpiwowar.argparser.utils.Introspection;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.mozilla.javascript.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import sf.net.experimaestro.connectors.*;
import sf.net.experimaestro.exceptions.*;
import sf.net.experimaestro.manager.*;
import sf.net.experimaestro.manager.java.JavaTasksIntrospection;
import sf.net.experimaestro.manager.js.object.JSCommand;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonObject;
import sf.net.experimaestro.manager.json.JsonResource;
import sf.net.experimaestro.manager.json.JsonString;
import sf.net.experimaestro.manager.plans.Constant;
import sf.net.experimaestro.scheduler.*;
import sf.net.experimaestro.server.TasksServlet;
import sf.net.experimaestro.utils.Cleaner;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.Output;
import sf.net.experimaestro.utils.XMLUtils;
import sf.net.experimaestro.utils.io.LoggerPrintWriter;
import sf.net.experimaestro.utils.log.Logger;

import javax.xml.xpath.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static java.lang.String.format;
import static sf.net.experimaestro.utils.JSUtils.unwrap;

/**
 * This class contains both utility static methods and functions that can be
 * called from javascript
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class XPMObject {

    /**
     * The filename used to the store the signature in generated directory names
     */
    public static final String XPM_SIGNATURE = ".xpm-signature";
    public static final String COMMAND_LINE_JOB_HELP = "Schedule a command line job.<br>The options are <dl>" +
            "<dt>launcher</dt><dd></dd>" +
            "<dt>stdin</dt><dd></dd>" +
            "<dt>stdout</dt><dd></dd>" +
            "<dt>lock</dt><dd>An array of couples (resource, lock type). The lock depends on the resource" +
            "at hand, but are generally READ, WRITE, EXCLUSIVE.</dd>" +
            "";
    public static final String DEFAULT_GROUP = "XPM_DEFAULT_GROUP";

    final static private Logger LOGGER = Logger.getLogger();
    static HashSet<String> COMMAND_LINE_OPTIONS = new HashSet<>(ImmutableSet.of("stdin", "stdout", "lock"));
    /**
     * Logging should be directed to an output
     */
    final Hierarchy loggerRepository;
    /**
     * The experiment repository
     */
    private final Repository repository;
    /**
     * Our scope (global among javascripts)
     */
    final Scriptable scope;
    /**
     * The task scheduler
     */
    private final Scheduler scheduler;
    /**
     * The environment
     */
    private final Map<String, String> environment;
    /**
     * The resource cleaner
     * <p/>
     * Used to close objects at the end of the execution of a script
     */
    private final Cleaner cleaner;
    /**
     * The connector for default inclusion
     */
    ResourceLocator currentResourceLocator;

    /**
     * Properties set by the script that will be returned
     */
    Map<String, Object> properties = new HashMap<>();
    /**
     * Default group for new jobs
     */
    String defaultGroup = "";
    /**
     * Default locks for new jobs
     */
    Map<Resource<?>, Object> defaultLocks = new TreeMap<>(Resource.ID_COMPARATOR);
    /**
     * List of submitted jobs (so that we don't submit them twice with the same script
     * by default)
     */
    Map<ResourceLocator, Resource> submittedJobs = new HashMap<>();
    /**
     * Simulate flags: jobs will not be submitted (but commands will be evaluated)
     */
    boolean _simulate;
    /**
     * Task context for this XPM object
     */
    private TaskContext taskContext;
    /**
     * The current work dir
     */
    private Holder<FileObject> workdir;
    /**
     * The context (local)
     */
    private Context context;
    /**
     * Root logger
     */
    private Logger rootLogger;




    /**
     * Initialise a new XPM object
     *
     * @param currentResourceLocator The xpath to the current script
     * @param context                The JS context
     * @param environment            The environment variables
     * @param scope                  The JS scope for execution
     * @param repository             The task repository
     * @param scheduler              The job scheduler
     * @param loggerRepository       The logger for the script
     * @param workdir                The working directory or null if none
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws InvocationTargetException
     * @throws SecurityException
     * @throws NoSuchMethodException
     */
    XPMObject(ResourceLocator currentResourceLocator,
              Context context,
              Map<String, String> environment,
              Scriptable scope,
              Repository repository,
              Scheduler scheduler,
              Hierarchy loggerRepository,
              Cleaner cleaner,
              Holder<FileObject> workdir)
            throws IllegalAccessException, InstantiationException,
            InvocationTargetException, SecurityException, NoSuchMethodException {
        LOGGER.debug("Current script is %s", currentResourceLocator);
        this.currentResourceLocator = currentResourceLocator;
        this.context = context;
        this.environment = environment;
        this.scope = scope;
        this.repository = repository;
        this.scheduler = scheduler;
        this.loggerRepository = loggerRepository;
        this.cleaner = cleaner;
        this.workdir = workdir == null ? new Holder<>(null) : workdir;
        this.rootLogger = Logger.getLogger(loggerRepository);


        context.setWrapFactory(JSBaseObject.XPMWrapFactory.INSTANCE);

        // --- Add new objects

        // Add functions from our Function object
        Map<String, ArrayList<Method>> functionsMap = JSBaseObject.analyzeClass(XPMFunctions.class).methods;
        final XPMFunctions xpmFunctions = new XPMFunctions(this);
        for (Map.Entry<String, ArrayList<Method>> entry : functionsMap.entrySet()) {
            MethodFunction function = new MethodFunction(entry.getKey());
            function.add(xpmFunctions, entry.getValue());
            ScriptableObject.putProperty(scope, entry.getKey(), function);
        }

        // tasks object
        XPMContext.addNewObject(context, scope, "tasks", "Tasks", new Object[]{this});

        // logger
        XPMContext.addNewObject(context, scope, "logger", JSBaseObject.getClassName(JSLogger.class), new Object[]{this, "xpm"});

        // xpm object
        XPMContext.addNewObject(context, scope, "xpm", "XPM", new Object[]{});

        ((JSXPM) get(scope, "xpm")).set(this);
        // --- Get the default group from the environment
        if (environment.containsKey(DEFAULT_GROUP))
            defaultGroup = environment.get(DEFAULT_GROUP);

    }

    static XPMObject getXPMObject(Scriptable scope) {
        while (scope.getParentScope() != null)
            scope = scope.getParentScope();
        return ((JSXPM) scope.get("xpm", scope)).xpm;
    }



    static XPMObject include(Context cx, Scriptable thisObj, Object[] args,
                             Function funObj, boolean repositoryMode) throws Exception {
        XPMObject xpm = getXPM(thisObj);

        if (args.length == 1)
            // Use the current connector
            return xpm.include(Context.toString(args[0]), repositoryMode);
        else if (args.length == 2)
            // Use the supplied connector
            return xpm.include(args[0], Context.toString(args[1]), repositoryMode);
        else
            throw new IllegalArgumentException("includeRepository expects one or two arguments");

    }

    /**
     * Retrievs the XPMObject from the JavaScript context
     */
    public static XPMObject getXPM(Scriptable thisObj) {
        if (thisObj instanceof NativeCall) {
            // XPM cannot be found if the scope is a native call object
            thisObj = thisObj.getParentScope();
        }
        return ((JSXPM) thisObj.get("xpm", thisObj)).xpm;
    }

    /**
     * Javascript constructor calling {@linkplain #include(String, boolean)}
     */
    static public Map<String, Object> js_include_repository(Context cx, Scriptable thisObj, Object[] args,
                                                            Function funObj) throws Exception {

        final XPMObject xpmObject = include(cx, thisObj, args, funObj, true);
        return xpmObject.properties;
    }

    /**
     * Javascript constructor calling {@linkplain #include(String, boolean)}
     */
    static public void js_include(Context cx, Scriptable thisObj, Object[] args,
                                  Function funObj) throws Exception {

        include(cx, thisObj, args, funObj, false);
    }

    /**
     * Returns a JSFileObject that corresponds to the path. This can
     * be used when building command lines containing path to resources
     * or executables
     *
     * @return A {@JSFileObject}
     */
    @JSHelp("Returns a FileObject corresponding to the path")
    static public Object js_path(Context cx, Scriptable thisObj, Object[] args, Function funObj) throws FileSystemException {
        if (args.length != 1)
            throw new IllegalArgumentException("path() needs one argument");

        XPMObject xpm = getXPM(thisObj);

        if (args[0] instanceof JSFileObject)
            return args[0];

        final Object o = unwrap(args[0]);

        if (o instanceof JSFileObject)
            return o;

        if (o instanceof FileObject)
            return xpm.newObject(JSFileObject.class, o);

        if (o instanceof String)
            return xpm.newObject(JSFileObject.class, xpm.currentResourceLocator.resolvePath(o.toString(), true).getFile());

        throw new XPMRuntimeException("Cannot convert type [%s] to a file xpath", o.getClass().toString());
    }

    @JSHelp(
            value = "Format a string",
            arguments = @JSArguments({
                    @JSArgument(name = "format", type = "String", help = "The string used to format"),
                    @JSArgument(name = "arguments...", type = "Object", help = "A list of objects")
            })
    )
    static public String js_format(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        if (args.length == 0)
            return "";

        Object fargs[] = new Object[args.length - 1];
        for (int i = 1; i < args.length; i++)
            fargs[i - 1] = unwrap(args[i]);
        String format = JSUtils.toString(args[0]);
        return String.format(format, fargs);
    }

    /**
     * Returns an XML element that corresponds to the wrapped value
     *
     * @return An XML element
     */
    static public Object js_value(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        if (args.length != 1)
            throw new IllegalArgumentException("value() needs one argument");
        final Object object = unwrap(args[0]);

        Document doc = XMLUtils.newDocument();
        XPMObject xpm = getXPM(thisObj);
        return JSUtils.domToE4X(doc.createElement(JSUtils.toString(object)), xpm.context, xpm.scope);

    }

    /**
     * Sets the current workdir
     */
    static public void js_set_workdir(Context cx, Scriptable thisObj, Object[] args, Function funObj) throws FileSystemException {
        XPMObject xpm = getXPM(thisObj);
        xpm.workdir.set(((JSFileObject) js_path(cx, thisObj, args, funObj)).getFile());
    }

    /**
     * Returns the current script location
     */
    static public JSFileObject js_script_file(Context cx, Scriptable thisObj, Object[] args, Function funObj)
            throws FileSystemException {
        if (args.length != 0)
            throw new IllegalArgumentException("script_file() has no argument");

        XPMObject xpm = getXPM(thisObj);

        return new JSFileObject(xpm.currentResourceLocator.getFile());
    }

    @JSHelp(value = "Returns a file relative to the current connector")
    public static Scriptable js_file(Context cx, Scriptable thisObj, Object[] args, Function funObj) throws FileSystemException {
        XPMObject xpm = getXPM(thisObj);
        if (args.length != 1)
            throw new IllegalArgumentException("file() takes only one argument");
        final String arg = JSUtils.toString(args[0]);
        return xpm.context.newObject(xpm.scope, JSFileObject.JSCLASSNAME,
                new Object[]{xpm.currentResourceLocator.getFile().getParent().resolveFile(arg)});
    }

    @JSHelp(value = "Unwrap an annotated XML value into a native JS object")
    public static Object js_unwrap(Object object) {
        return object.toString();
    }

    /**
     * Returns a QName object
     *
     * @param ns        The namespace: can be the URI string, or a javascript
     *                  Namespace object
     * @param localName the localname
     * @return a QName object
     */
    static public Object js_qname(Object ns, String localName) {
        // First unwrapToObject the object
        if (ns instanceof Wrapper)
            ns = ((Wrapper) ns).unwrap();

        // If ns is a javascript Namespace object
        if (ns instanceof ScriptableObject) {
            ScriptableObject scriptableObject = (ScriptableObject) ns;
            if (scriptableObject.getClassName().equals("Namespace")) {
                Object object = scriptableObject.get("uri", null);
                return new QName(object.toString(), localName);
            }
        }

        // If ns is a string
        if (ns instanceof String)
            return new QName((String) ns, localName);

        throw new XPMRuntimeException("Not implemented (%s)", ns.getClass());
    }

    public static Object get(Scriptable scope, final String name) {
        Object object = scope.get(name, scope);
        if (object != null && object == Undefined.instance)
            object = null;
        else if (object instanceof Wrapper)
            object = ((Wrapper) object).unwrap();
        return object;
    }

    /**
     * Runs an XPath
     *
     * @param path
     * @param xml
     * @return
     * @throws javax.xml.xpath.XPathExpressionException
     */
    static public Object js_xpath(String path, Object xml)
            throws XPathExpressionException {
        Node dom = (Node) JSUtils.toDOM(null, xml);
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new NSContext(dom));
        XPathFunctionResolver old = xpath.getXPathFunctionResolver();
        xpath.setXPathFunctionResolver(new XPMXPathFunctionResolver(old));

        XPathExpression expression = xpath.compile(path);
        String list = (String) expression.evaluate(
                dom instanceof Document ? ((Document) dom).getDocumentElement()
                        : dom, XPathConstants.STRING);
        return list;
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

    static String toString(Object object) {
        if (object instanceof NativeJavaObject)
            return ((NativeJavaObject) object).unwrap().toString();
        return object.toString();
    }

    private static FileObject getFileObject(Connector connector, Object stdout) throws FileSystemException {
        if (stdout instanceof String || stdout instanceof ConsString)
            return connector.getMainConnector().resolveFile(stdout.toString());

        if (stdout instanceof JSFileObject)
            return connector.getMainConnector().resolveFile(stdout.toString());

        if (stdout instanceof FileObject)
            return (FileObject) stdout;

        throw new XPMRuntimeException("Unsupported stdout type [%s]", stdout.getClass());
    }

    /**
     * Clone properties from this XPM instance
     */
    private XPMObject clone(ResourceLocator scriptpath, Scriptable scriptScope, TreeMap<String, String> newEnvironment) throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        final XPMObject clone = new XPMObject(scriptpath, context, newEnvironment, scriptScope, repository, scheduler, loggerRepository, cleaner, workdir);
        clone.defaultGroup = this.defaultGroup;
        clone.defaultLocks.putAll(this.defaultLocks);
        clone.submittedJobs = new HashMap<>(this.submittedJobs);
        clone._simulate = _simulate;
        return clone;
    }

    public Logger getRootLogger() {
        return rootLogger;
    }

    private boolean simulate() {
        return _simulate || (taskContext != null && taskContext.simulate());
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

        return include(new ResourceLocator(connector, path), repositoryMode);
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
        ResourceLocator scriptpath = currentResourceLocator.resolvePath(path, true);
        LOGGER.debug("Including repository file [%s]", scriptpath);
        return include(scriptpath, repositoryMode);
    }

    /**
     * Central method called for any script inclusion
     *
     * @param scriptLocator The path to the script
     * @param repositoryMode If true, runs in a separate environement
     * @throws Exception if something goes wrong
     */
    private XPMObject include(ResourceLocator scriptLocator, boolean repositoryMode) throws Exception {

        try (InputStream inputStream = scriptLocator.getFile().getContent().getInputStream()) {
            Scriptable scriptScope = scope;
            XPMObject xpmObject = this;
            if (repositoryMode) {
                // Run the script in a new environment
                scriptScope = XPMContext.newScope();
                final TreeMap<String, String> newEnvironment = new TreeMap<>(environment);
                xpmObject = clone(scriptLocator, scriptScope, newEnvironment);
                threadXPM.set(xpmObject);
            }

            // Avoid adding the protocol if this is a local file
            final String sourceName = scriptLocator.getConnector() == LocalhostConnector.getInstance()
                    ? scriptLocator.getPath() : scriptLocator.toString();

            Context.getCurrentContext().evaluateReader(scriptScope, new InputStreamReader(inputStream), sourceName, 1, null);

            return xpmObject;
        } catch(FileNotFoundException e) {
            throw new XPMRhinoException("File not found: %s", scriptLocator.getFile());
        } finally {
            threadXPM.set(this);
        }

    }

    /**
     * Creates a new JavaScript object
     */
    Scriptable newObject(Class<?> aClass, Object... arguments) {
        return context.newObject(scope, JSBaseObject.getClassName(aClass), arguments);
    }

    /**
     * Get the information about a given task
     *
     * @param namespace The namespace
     * @param id        The ID within the namespace
     * @return
     */
    public Scriptable getTaskFactory(String namespace, String id) {
        TaskFactory factory = repository.getFactory(new QName(namespace, id));
        LOGGER.debug("Creating a new JS task factory %s", factory.getId());
        return context.newObject(scope, "TaskFactory",
                new Object[]{Context.javaToJS(factory, scope)});
    }

    /**
     * Get the information about a given task
     *
     * @param localPart
     * @return
     */
    public Scriptable getTask(String namespace, String localPart) {
        return getTask(new QName(namespace, localPart));
    }

    public Scriptable getTask(QName qname) {
        TaskFactory factory = repository.getFactory(qname);
        if (factory == null)
            throw new XPMRuntimeException("Could not find a task with name [%s]", qname);
        LOGGER.info("Creating a new JS task [%s]", factory.getId());
        return new JSTaskWrapper(factory.create(), this);
    }

    /**
     * Simple evaluation of shell commands (does not create a job)
     *
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public String evaluate(Object jsargs, NativeObject options) throws Exception {
        Command command = JSCommand.getCommand(jsargs);

        // Run the process and captures the output
        final SingleHostConnector connector = currentResourceLocator.getConnector().getConnector(null);
        AbstractProcessBuilder builder = connector.processBuilder();


        try (CommandEnvironment commandEnv = new CommandEnvironment.Temporary(connector)) {
            // Transform the list
            builder.command(Lists.newArrayList(Iterables.transform(command.list(), argument -> {
                try {
                    return ((Command) argument).prepare(commandEnv);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            })));

            if (options != null && options.has("stdout", options)) {
                FileObject stdout = getFileObject(connector, unwrap(options.get("stdout", options)));
                builder.redirectOutput(AbstractCommandBuilder.Redirect.to(stdout));
            } else {
                builder.redirectOutput(AbstractCommandBuilder.Redirect.PIPE);
            }

            builder.redirectError(AbstractCommandBuilder.Redirect.PIPE);


            builder.detach(false);
            builder.environment(environment);

            XPMProcess p = builder.start();

            new Thread("stderr") {
                BufferedReader errorStream = new BufferedReader(new InputStreamReader(p.getErrorStream()));

                @Override
                public void run() {
                    errorStream.lines().forEach(line -> getRootLogger().info(line));
                }
            }.start();


            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            int len = 0;
            char[] buffer = new char[8192];
            StringBuilder sb = new StringBuilder();
            while ((len = input.read(buffer, 0, buffer.length)) >= 0) {
                sb.append(buffer, 0, len);
            }
            input.close();

            int error = p.waitFor();
            if (error != 0) {
                throw new XPMRhinoException("Error while evaluating command");
            }
            return sb.toString();
        }
    }

    /**
     * Log a message to be returned to the client
     */
    public void log(String format, Object... objects) {
        rootLogger.info(format, objects);
    }

    /**
     * Log a message to be returned to the client
     */
    public void warning(String format, Object... objects) {
        RhinoException rhinoException = new XPMRhinoException();

        rootLogger.warn(String.format(format, objects) + " in " + rhinoException.getScriptStack()[0]);
    }

    /**
     * Get a QName
     */
    public QName qName(String namespaceURI, String localPart) {
        return new QName(namespaceURI, localPart);
    }

    // XML Utilities

    /**
     * Get experimaestro namespace
     */
    public String ns() {
        return Manager.EXPERIMAESTRO_NS;
    }

    public Object domToE4X(Node node) {
        return JSUtils.domToE4X(node, context, scope);
    }

    public String xmlToString(Node node) {
        return XMLUtils.toString(node);
    }

    /**
     * Creates a new command line job
     *
     * @param path     The identifier for this job
     * @param commands The command line(s)
     * @param options  The options
     * @return
     * @throws Exception
     */
    public JSResource commandlineJob(Object path, Commands commands, NativeObject options) throws Exception {
        CommandLineTask task = null;
        // --- XPMProcess arguments: convert the javascript array into a Java array
        // of String
        LOGGER.debug("Adding command line job");

        // --- Create the task


        final Connector connector;

        if (options != null && options.has("connector", options)) {
            connector = ((JSConnector) options.get("connector", options)).getConnector();
        } else {
            connector = currentResourceLocator.getConnector();
        }

        // Store connector in database
        scheduler.put(connector);

        // Resolve the path for the given connector
        if (path instanceof FileObject) {
            path = connector.getMainConnector().resolve((FileObject) path);
        } else
            path = connector.getMainConnector().resolve(path.toString());

        final ResourceLocator locator = new ResourceLocator(connector.getMainConnector(), path.toString());
        task = new CommandLineTask(scheduler, locator, commands);

        if (submittedJobs.containsKey(locator)) {
            getRootLogger().info("Not submitting %s [duplicate]", locator);
            if (simulate())
                return new JSResource(submittedJobs.get(locator));
            return new JSResource(scheduler.getResource(locator));
        }

        // -- Adds default locks
        Map<? extends Resource, ?> _defaultLocks = taskContext != null && taskContext.defaultLocks() != null
                ? taskContext.defaultLocks() : defaultLocks;
        for (Map.Entry<? extends Resource, ?> lock : _defaultLocks.entrySet()) {
            Dependency dependency = lock.getKey().createDependency(lock.getValue());
            task.addDependency(dependency);
        }


        // --- Environment
        task.environment = new TreeMap<>(environment);

        // --- Options


        if (options != null) {

            final ArrayList unmatched = new ArrayList(Sets.difference(options.keySet(), COMMAND_LINE_OPTIONS));
            if (!unmatched.isEmpty()) {
                throw new IllegalArgumentException(format("Some options are not allowed: %s",
                        Output.toString(", ", unmatched)));
            }


            // --- XPMProcess launcher
            if (options.has("launcher", options)) {
                final Object launcher = options.get("launcher", options);
                if (launcher != null && !(launcher instanceof UniqueTag))
                    task.setLauncher(((JSLauncher) launcher).getLauncher());

            }

            // --- Redirect standard output
            if (options.has("stdin", options)) {
                final Object stdin = unwrap(options.get("stdin", options));
                if (stdin instanceof String || stdin instanceof ConsString) {
                    task.setInput(stdin.toString());
                } else if (stdin instanceof FileObject) {
                    task.setInput((FileObject) stdin);
                } else
                    throw new XPMRuntimeException("Unsupported stdin type [%s]", stdin.getClass());
            }

            // --- Redirect standard output
            if (options.has("stdout", options)) {
                FileObject fileObject = getFileObject(connector, unwrap(options.get("stdout", options)));
                task.setOutput(fileObject);
            }

            // --- Redirect standard error
            if (options.has("stderr", options)) {
                FileObject fileObject = getFileObject(connector, unwrap(options.get("stderr", options)));
                task.setError(fileObject);
            }


            // --- Resources to lock
            if (options.has("lock", options)) {
                List locks = (List) options.get("lock", options);
                for (int i = (int) locks.size(); --i >= 0; ) {
                    Object lock_i = JSUtils.unwrap(locks.get(i));
                    Dependency dependency = null;

                    if (lock_i instanceof Dependency) {
                        dependency = (Dependency) lock_i;
                    } else if (lock_i instanceof NativeArray) {
                        NativeArray array = (NativeArray) lock_i;
                        if (array.getLength() != 2)
                            throw new XPMRhinoException(new IllegalArgumentException("Wrong number of arguments for lock"));

                        final Object depObject = JSUtils.unwrap(array.get(0, array));
                        Resource resource = null;
                        if (depObject instanceof Resource) {
                            resource = (Resource) depObject;
                        } else {
                            final String rsrcPath = Context.toString(depObject);
                            ResourceLocator depLocator = ResourceLocator.parse(rsrcPath);
                            resource = scheduler.getResource(depLocator);
                            if (resource == null)
                                if (simulate()) {
                                    if (!submittedJobs.containsKey(depLocator))
                                        LOGGER.error("The dependency [%s] cannot be found", depLocator);
                                } else {
                                    throw new XPMRuntimeException("Resource [%s] was not found", rsrcPath);
                                }
                        }

                        final Object lockType = array.get(1, array);
                        LOGGER.debug("Adding dependency on [%s] of type [%s]", resource, lockType);

                        if (!simulate()) {
                            dependency = resource.createDependency(lockType);
                        }
                    } else {
                        throw new XPMRuntimeException("Element %d for option 'lock' is not a dependency but %s",
                                i, lock_i.getClass());
                    }

                    if (!simulate()) {
                        task.addDependency(dependency);
                    }
                }

            }


        }

        // Update the task status now that it is initialized
        task.setGroup(defaultGroup);

        final Resource old = scheduler.getResource(locator);
        if (old != null) {
            // TODO: if equal, do not try to replace the task
            if (!task.replace(old)) {
                getRootLogger().warn(String.format("Cannot override resource [%s]", task.getIdentifier()));
                old.init(scheduler);
                return new JSResource(old);
            } else {
                getRootLogger().info(String.format("Overwriting resource [%s]", task.getIdentifier()));
            }
        }

        task.setState(ResourceState.WAITING);
        if (simulate()) {
            PrintWriter pw = new LoggerPrintWriter(getRootLogger(), Level.INFO);
            pw.format("[SIMULATE] Starting job: %s%n", task.toString());
            pw.format("Command: %s%n", task.getCommands().toString());
            pw.format("Locator: %s", locator.toString());
            pw.flush();
        } else {
            scheduler.store(task, false);
        }

        return new JSResource(task);
    }

    public void register(Closeable closeable) {
        cleaner.register(closeable);
    }

    public void unregister(AutoCloseable autoCloseable) {
        cleaner.unregister(autoCloseable);
    }

    Repository getRepository() {
        return repository;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public TaskContext newTaskContext() {
        return new TaskContext(scheduler, currentResourceLocator, workdir.get(), getRootLogger());
    }

    public void setLocator(ResourceLocator locator) {
        this.currentResourceLocator = locator;
    }

    public void setTaskContext(TaskContext taskContext) {
        this.taskContext = taskContext;
    }

    /**
     * Creates a unique (up to the collision probability) ID based on the hash
     *
     * @param basedir
     * @param prefix     The prefix for the directory
     * @param id         The task ID or any other QName
     * @param jsonValues the JSON object from which the hash is computed
     * @return
     */
    public JSFileObject uniqueDirectory(Scriptable scope, FileObject basedir, String prefix, QName id, Object jsonValues) throws IOException, NoSuchAlgorithmException {
        if (basedir == null) {
            if (workdir.get() == null)
                throw new XPMRuntimeException("Working directory was not set before unique_directory() is called");

            basedir = workdir.get();
        }
        final Json json = JSUtils.toJSON(scope, jsonValues);
        return new JSFileObject(Manager.uniqueDirectory(basedir, prefix, id, json));
    }

    public Connector getConnector() {
        return currentResourceLocator.getConnector();
    }

    final static ThreadLocal<XPMObject> threadXPM = new ThreadLocal<>();

    public static XPMObject getThreadXPM() {
        return threadXPM.get();
    }

    static public class Holder<T> {
        private T value;

        Holder(T value) {
            this.value = value;
        }

        T get() {
            return value;
        }

        void set(T value) {
            this.value = value;
        }
    }


    // --- Javascript methods

    static public class JSXPM extends JSBaseObject {
        XPMObject xpm;

        @JSFunction
        public JSXPM() {
        }

        static public void log(Level level, Context cx, Scriptable thisObj, Object[] args, Function funObj) {
            if (args.length < 1)
                throw new XPMRuntimeException("There should be at least one argument for log()");

            String format = Context.toString(args[0]);
            Object[] objects = new Object[args.length - 1];
            for (int i = 1; i < args.length; i++)
                objects[i - 1] = unwrap(args[i]);

            ((JSXPM) thisObj).xpm.log(format, objects);
        }

        protected void set(XPMObject xpm) {
            this.xpm = xpm;
        }

        @Override
        public String getClassName() {
            return "XPM";
        }

        @JSFunction("set_property")
        public void setProperty(String name, Object object) {
            final Object x = unwrap(object);
            xpm.properties.put(name, object);
        }

        @JSFunction("set_default_group")
        @JSHelp("Set the default group for new tasks")
        public void setDefaultGroup(String name) {
            xpm.defaultGroup = name;
        }

        @JSFunction("set_default_lock")
        @JSHelp("Adds a new resource to lock for all jobs to be started")
        public void setDefaultLock(Object resource, Object parameters) {
            xpm.defaultLocks.put((Resource) unwrap(resource), parameters);
        }

        @JSFunction("token_resource")
        @JSHelp("Retrieve (or creates) a token resource with a given xpath")
        public Scriptable getTokenResource(
                @JSArgument(name = "path", help = "The path of the resource") String path
        ) throws ExperimaestroCannotOverwrite {
            final ResourceLocator locator = new ResourceLocator(XPMConnector.getInstance(), path);
            final Resource resource = xpm.scheduler.getResource(locator);
            final TokenResource tokenResource;
            if (resource == null) {
                tokenResource = new TokenResource(xpm.scheduler, new ResourceData(locator), 0);
                tokenResource.init(xpm.scheduler);
                xpm.scheduler.store(tokenResource, false);
            } else {
                if (!(resource instanceof TokenResource))
                    throw new AssertionError(String.format("Resource %s exists and is not a token", path));
                tokenResource = (TokenResource) resource;
            }

            return xpm.context.newObject(xpm.scope, "TokenResource", new Object[]{tokenResource});
        }

        @JSFunction()
        public void log() {

        }

        @JSFunction("logger")
        public Scriptable getLogger(String name) {
            return xpm.newObject(JSLogger.class, xpm, name);
        }


        @JSFunction("log_level")
        @JSHelp(value = "Sets the logger debug level")
        public void setLogLevel(
                @JSArgument(name = "name") String name,
                @JSArgument(name = "level") String level
        ) {
            Logger.getLogger(xpm.loggerRepository, name).setLevel(Level.toLevel(level));
        }


        @JSFunction("get_script_path")
        public String getScriptPath() {
            return xpm.currentResourceLocator.getPath();
        }

        @JSFunction("get_script_file")
        public Scriptable getScriptFile() throws FileSystemException {
            return xpm.newObject(JSFileObject.class, xpm.currentResourceLocator.getFile());
        }

        /**
         * Add a module
         */
        @JSFunction("add_module")
        public JSModule addModule(Object object) {
            JSModule module = new JSModule(xpm, xpm.repository, xpm.scope, (NativeObject) object);
            LOGGER.debug("Adding module [%s]", module.module.getId());
            xpm.repository.addModule(module.module);
            return module;
        }

        /**
         * Add an experiment
         *
         * @param object
         * @return
         */
        @JSFunction("add_task_factory")
        public Scriptable add_task_factory(NativeObject object) throws ValueMismatchException {
            JSTaskFactory factory = new JSTaskFactory(xpm.scope, object, xpm.repository);
            xpm.repository.addFactory(factory.factory);
            return xpm.context.newObject(xpm.scope, "TaskFactory",
                    new Object[]{factory});
        }

        @JSFunction("get_task")
        public Scriptable getTask(QName name) {
            return xpm.getTask(name);
        }

        @JSFunction("get_task")
        public Scriptable getTask(
                String namespaceURI,
                String localName) {
            return xpm.getTask(namespaceURI, localName);
        }


        @JSFunction(value = "evaluate", optional = 1)
        public String evaluate(
                NativeArray command,
                NativeObject options
        ) throws Exception {
            return xpm.evaluate(command, options);
        }

        @JSFunction("file")
        @JSHelp(value = "Returns a file relative to the current connector")
        public Scriptable file(@JSArgument(name = "filepath") String filepath) throws FileSystemException {
            return xpm.context.newObject(xpm.scope, JSFileObject.JSCLASSNAME,
                    new Object[]{xpm, xpm.currentResourceLocator.resolvePath(filepath).getFile()});
        }

        @JSFunction
        public Scriptable file(@JSArgument(name = "file") JSFileObject file) throws FileSystemException {
            return file;
        }


        @JSFunction(value = "command_line_job", optional = 1)
        @JSHelp(value = COMMAND_LINE_JOB_HELP)
        public Scriptable commandlineJob(@JSArgument(name = "jobId") Object path,
                                         @JSArgument(type = "Array", name = "command") NativeArray jsargs,
                                         @JSArgument(type = "Map", name = "options") NativeObject jsoptions) throws Exception {
            Commands commands = new Commands(JSCommand.getCommand(jsargs));
            JSResource jsResource = xpm.commandlineJob(path, commands, jsoptions);
            return jsResource;
        }

        @JSFunction(value = "command_line_job", optional = 1)
        @JSHelp(value = COMMAND_LINE_JOB_HELP)
        public Scriptable commandlineJob(@JSArgument(name = "jobId") Object path,
                                         @JSArgument(type = "Array", name = "command") Command command,
                                         @JSArgument(type = "Map", name = "options") NativeObject jsoptions) throws Exception {
            Commands commands = new Commands(command);
            JSResource jsResource = xpm.commandlineJob(path, commands, jsoptions);
            return jsResource;
        }

        @JSFunction(value = "command_line_job", optional = 1)
        @JSHelp(value = COMMAND_LINE_JOB_HELP)
        public Scriptable commandlineJob(@JSArgument(name = "jobId") Object jobId,
                                         Commands commands,
                                         @JSArgument(type = "Map", name = "options") NativeObject jsoptions) throws Exception {
            JSResource jsResource = xpm.commandlineJob(jobId, commands, jsoptions);
            return jsResource;
        }

        @JSFunction(value = "command_line_job", optional = 1)
        @JSHelp(value = COMMAND_LINE_JOB_HELP)
        public Scriptable commandlineJob(
                JsonObject json,
                @JSArgument(name = "jobId") Object jobId,
                Object commands,
                @JSArgument(type = "Map", name = "options") NativeObject jsOptions) throws Exception {

            Commands _commands;
            if (commands instanceof Commands) {
                _commands = (Commands) commands;
            } else if (commands instanceof Command) {
                _commands = new Commands((Command) commands);
            } else if (commands instanceof NativeArray) {
                _commands = new Commands(JSCommand.getCommand(commands));
            } else {
                throw new XPMRhinoIllegalArgumentException("2nd argument of command_line_job must be a command");
            }

            JSResource jsResource = xpm.commandlineJob(jobId, _commands, jsOptions);

            // Update the json
            json.put(Manager.XP_RESOURCE.toString(), new JsonResource((Resource) jsResource.unwrap()));
            return jsResource;
        }

        /**
         * Declare an alternative
         *
         * @param qname A qualified name
         */
        @JSFunction("declare_alternative")
        @JSHelp(value = "Declare a qualified name as an alternative input")
        public void declareAlternative(Object qname) {
            AlternativeType type = new AlternativeType((QName) qname);
            xpm.repository.addType(type);
        }


        /**
         * Useful for debugging E4X: outputs the DOM view
         *
         * @param xml an E4X object
         */
        @JSFunction("output_e4x")
        @JSHelp("Outputs the E4X XML object")
        public void outputE4X(@JSArgument(name = "xml", help = "The XML object") Object xml) {
            final Iterable<? extends Node> list = JSCommand.xmlAsList(JSUtils.toDOM(null, xml));
            for (Node node : list) {
                output(node);
            }
        }

        @JSFunction("publish")
        @JSHelp("Publish the repository on the web server")
        public void publish() throws InterruptedException {
            TasksServlet.updateRepository(xpm.currentResourceLocator.toString(), xpm.repository);
        }

        @JSFunction
        @JSHelp("Set the simulate flag: When true, the jobs are not submitted but just output")
        public boolean simulate(boolean simulate) {
            boolean old = xpm._simulate;
            xpm._simulate = simulate;
            return simulate;
        }

        @JSFunction
        public boolean simulate() {
            return xpm._simulate;
        }

        @JSFunction
        public String env(String key, String value) {
            return xpm.environment.put(key, value);
        }

        @JSFunction
        public String env(String key) {
            return xpm.environment.get(key);
        }

        private void output(Node node) {
            switch (node.getNodeType()) {
                case Node.ELEMENT_NODE:
                    xpm.log("[element %s]", node.getNodeName());
                    for (Node child : XMLUtils.children(node))
                        output(child);
                    xpm.log("[/element %s]", node.getNodeName());
                    break;
                case Node.TEXT_NODE:
                    xpm.log("text [%s]", node.getTextContent());
                    break;
                default:
                    xpm.log("%s", node.toString());
            }
        }
    }


    static class XPMFunctions {
        XPMObject xpm;

        @JSFunction
        public XPMFunctions(XPMObject xpm) {
            this.xpm = xpm;
        }

        @JSFunction(scope = true, value = "merge")
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


        @JSFunction(scope = true)
        public static String digest(Context cx, Scriptable scope, Object... jsons) throws NoSuchAlgorithmException, IOException {
            Json json = JSUtils.toJSON(scope, jsons);
            return Manager.getDigest(json);
        }

        @JSFunction(scope = true)
        public static String descriptor(Context cx, Scriptable scope, Object... jsons) throws NoSuchAlgorithmException, IOException {
            Json json = JSUtils.toJSON(scope, jsons);
            return Manager.getDescriptor(json);
        }

        @JSFunction(scope = true)
        @JSHelp(value = "Transform plans outputs with a function")
        public static Scriptable transform(Context cx, Scriptable scope, Callable f, JSAbstractOperator... operators) throws FileSystemException {
            return new JSTransform(cx, scope, f, operators);
        }

        @JSFunction
        public static JSInput input(String name) {
            return new JSInput(name);
        }

        @JSFunction(value = "_")
        @JSDeprecated
        public static Object _get_value(Object object) {
            return get_value(object);
        }

        @JSFunction("$")
        public static Object get_value(Object object) {
            object = unwrap(object);
            if (object instanceof Json)
                return ((Json) object).get();

            return object;
        }

        @JSFunction("assert")
        public static void _assert(boolean condition, String format, Object... objects) {
            if (!condition)
                throw new EvaluatorException("assertion failed: " + String.format(format, objects));
        }

        @JSFunction()
        @JSHelp("Get a lock over all the resources defined in a JSON object")
        public NativeArray get_locks(String lockMode, JsonObject json) {
            ArrayList<Dependency> dependencies = new ArrayList<>();
            for (Json jsonEntry : json.values()) {
                if (jsonEntry instanceof JsonObject) {
                    final Resource resource = getResource((JsonObject) jsonEntry);
                    if (resource != null) {
                        final Dependency dependency = resource.createDependency(lockMode);
                        dependencies.add(dependency);
                    }
                }
            }

            return new NativeArray(dependencies.toArray(new Dependency[dependencies.size()]));
        }

        @JSFunction(value = "$$", scope = true)
        @JSHelp("Get the resource associated with the json object")
        public JSResource get_resource(Context cx, Scriptable scope, Json json) {
            Resource resource = null;
            if (json instanceof JsonObject) {
                resource = getResource((JsonObject) json);
            } else {
                throw new XPMRhinoException("Cannot handle Json of type " + json.getClass());
            }

            if (resource != null) {
                return new JSResource(resource);
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
                        return xpm.submittedJobs.get(uri);
                    } else {
                        return xpm.scheduler.getResource(ResourceLocator.parse(uri));
                    }
                }

            }
            return null;
        }

        @JSFunction(value = "java_repository", optional = 1, optionalsAtStart = true)
        @JSHelp("Include a repository from introspection of a java project")
        public void includeJavaRepository(Connector connector, String[] paths) throws IOException, ExperimaestroException, ClassNotFoundException {
            if (connector == null)
                connector = LocalhostConnector.getInstance();
            JavaTasksIntrospection.addToRepository(xpm.repository, connector, paths);
        }

    }

}