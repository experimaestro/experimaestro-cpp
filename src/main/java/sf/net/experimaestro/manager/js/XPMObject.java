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
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.mozilla.javascript.*;
import org.mozilla.javascript.annotations.JSFunction;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sf.net.experimaestro.connectors.*;
import sf.net.experimaestro.exceptions.ExperimaestroCannotOverwrite;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.manager.*;
import sf.net.experimaestro.scheduler.*;
import sf.net.experimaestro.server.TasksServlet;
import sf.net.experimaestro.utils.Cleaner;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.XMLUtils;
import sf.net.experimaestro.utils.log.Logger;

import javax.xml.xpath.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;

import static java.lang.String.format;

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

    final static private Logger LOGGER = Logger.getLogger();
    public static final String DEFAULT_GROUP = "XPM_DEFAULT_GROUP";

    /**
     * The experiment repository
     */
    private final Repository repository;

    /**
     * Our scope (global among javascripts)
     */
    private final Scriptable scope;

    /**
     * The context (local)
     */
    private Context context;

    /**
     * The task scheduler
     */
    private final Scheduler scheduler;

    /**
     * The environment
     */
    private final Map<String, String> environment;

    /**
     * The connector for default inclusion
     */
    ResourceLocator currentResourceLocator;

    /**
     * Properties set by the script that will be returned
     */
    Map<String, Object> properties = new HashMap<>();

    /**
     * Logging should be directed to an output
     */
    final Hierarchy loggerRepository;

    /**
     * Root logger
     */
    private Logger rootLogger;

    /**
     * Default group for new jobs
     */
    String defaultGroup = "";

    /**
     * Default locks for new jobs
     */
    Map<Resource, Object> defaultLocks = new TreeMap<>();

    /**
     * List of submitted jobs (so that we don't submit them twice with the same script
     * by default)
     */
    Set<ResourceLocator> submittedJobs = new HashSet<>();

    /**
     * The resource cleaner
     */
    private final Cleaner cleaner;

    static final JSUtils.FunctionDefinition[] definitions = {
            new JSUtils.FunctionDefinition(XPMObject.class, "qname", Object.class, String.class),
            new JSUtils.FunctionDefinition(XPMObject.class, "include", null),
            new JSUtils.FunctionDefinition(XPMObject.class, "include_repository", null),
            new JSUtils.FunctionDefinition(XPMObject.class, "script_file", null),
            new JSUtils.FunctionDefinition(XPMObject.class, "xpath", String.class, Object.class),
            new JSUtils.FunctionDefinition(XPMObject.class, "path", null),
            new JSUtils.FunctionDefinition(XPMObject.class, "value", null),
            new JSUtils.FunctionDefinition(XPMObject.class, "file", null),
            new JSUtils.FunctionDefinition(XPMObject.class, "format", null),
    };


    /**
     * Initialise a new XPM object
     *
     * @param currentResourceLocator The path to the current script
     * @param context                The JS context
     * @param environment            The environment variables
     * @param scope                  The JS scope for execution
     * @param repository             The task repository
     * @param scheduler              The job scheduler
     * @param loggerRepository       The logger for the script
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws InvocationTargetException
     * @throws SecurityException
     * @throws NoSuchMethodException
     */
    public XPMObject(ResourceLocator currentResourceLocator,
                     Context context,
                     Map<String, String> environment,
                     Scriptable scope,
                     Repository repository,
                     Scheduler scheduler,
                     Hierarchy loggerRepository,
                     Cleaner cleaner)
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
        this.rootLogger = Logger.getLogger(loggerRepository);


        context.setWrapFactory(JSObject.XPMWrapFactory.INSTANCE);

        // --- Tells E4X to preserve whitespaces
        // XML.ignoreWhitespace=false

        final Scriptable jsXML = (Scriptable) scope.get("XML", scope);
        scope.put("ignoreWhitespace", jsXML, false);

        // --- Define functions and classes

        // Define the new classes (scans the package for implementations of ScriptableObject)
        ArrayList<Class<?>> list = new ArrayList<>();

        try {
            final String packageName = getClass().getPackage().getName();
            final String resourceName =  packageName.replace('.', '/');
            final Enumeration<URL> urls = XPMObject.class.getClassLoader().getResources(resourceName);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                Introspection.addClasses(new Introspection.Checker() {
                    @Override
                    public boolean accepts(Class<?> aClass) {
                        return (ScriptableObject.class.isAssignableFrom(aClass) || JSObject.class.isAssignableFrom(aClass)) && ((aClass.getModifiers() & Modifier.ABSTRACT) == 0);
                    }
                }, list, packageName, -1, url);
            }
        } catch (IOException e) {
            LOGGER.error(e, "While trying to grab resources");
        }

        for (Class<?> aClass : list) {
            JSObject.defineClass(scope, (Class<? extends Scriptable>) aClass);
        }

        // Add functions
        for (JSUtils.FunctionDefinition definition : definitions)
            JSUtils.addFunction(scope, definition);

        // --- Add new objects

        // namespace
        addNewObject(context, scope, "xp", "Namespace", new Object[]{"xp",
                Manager.EXPERIMAESTRO_NS});

        // xpm object
        addNewObject(context, scope, "xpm", "XPM", new Object[]{});
        ((JSXPM) get(scope, "xpm")).set(this);

        // scheduler
        addNewObject(context, scope, "scheduler", "Scheduler",
                new Object[]{scheduler, this});

        // logger
        addNewObject(context, scope, "logger", JSObject.getClassName(JSLogger.class), new Object[]{this, "xpm"});

        // --- Get the default group from the environment
        if (environment.containsKey(DEFAULT_GROUP))
            defaultGroup = environment.get(DEFAULT_GROUP);

    }

    static XPMObject getXPMObject(Scriptable thisObj) {
        return ((JSXPM) thisObj.getParentScope().get("xpm", thisObj.getParentScope())).xpm;
    }


    /**
     * Clone properties from this XPM instance
     */
    private XPMObject clone(ResourceLocator scriptpath, Scriptable scriptScope, TreeMap<String, String> newEnvironment) throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        final XPMObject clone = new XPMObject(scriptpath, context, newEnvironment, scriptScope, repository, scheduler, loggerRepository, cleaner);
        clone.defaultGroup = this.defaultGroup;
        clone.defaultLocks = new TreeMap<>(this.defaultLocks);
        clone.submittedJobs = new HashSet<>(this.submittedJobs);
        return clone;
    }


    static private void addNewObject(Context cx, Scriptable scope,
                                     final String objectName, final String className,
                                     final Object[] params) {
        ScriptableObject.defineProperty(scope, objectName,
                cx.newObject(scope, className, params), 0);
    }


    public Logger getRootLogger() {
        return rootLogger;
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
     * @param path The path, absolute or relative to the current evaluated script
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public XPMObject include(String path, boolean repositoryMode) throws Exception {
        ResourceLocator scriptpath = currentResourceLocator.resolvePath(path, true);
        LOGGER.debug("Including repository file [%s]", scriptpath);
        return include(scriptpath, repositoryMode);
    }

    /**
     * Final method called for inclusion of a script
     *
     * @param scriptpath
     * @param repositoryMode If true, runs in a separate environement
     * @throws Exception
     */
    private XPMObject include(ResourceLocator scriptpath, boolean repositoryMode) throws Exception {

        try (InputStream inputStream = scriptpath.getFile().getContent().getInputStream()) {
            Scriptable scriptScope = scope;
            XPMObject xpmObject = this;
            if (repositoryMode) {
                // Run the script in a new environment
                scriptScope = context.initStandardObjects();
                final TreeMap<String, String> newEnvironment = new TreeMap<String, String>(environment);
                xpmObject = clone(scriptpath, scriptScope, newEnvironment);

            }

            Context.getCurrentContext().evaluateReader(scriptScope, new InputStreamReader(inputStream), scriptpath.toString(), 1, null);

            return xpmObject;
        }

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
    private static XPMObject getXPM(Scriptable thisObj) {
        return ((JSXPM) thisObj.get("xpm", thisObj)).xpm;
    }


    /**
     * Javascript method calling {@linkplain #include(String, boolean)}
     */
    static public Map<String, Object> js_include_repository(Context cx, Scriptable thisObj, Object[] args,
                                                            Function funObj) throws Exception {

        final XPMObject xpmObject = include(cx, thisObj, args, funObj, true);
        return xpmObject.properties;
    }

    /**
     * Javascript method calling {@linkplain #include(String, boolean)}
     */
    static public void js_include(Context cx, Scriptable thisObj, Object[] args,
                                  Function funObj) throws Exception {

        include(cx, thisObj, args, funObj, false);
    }

    /**
     * Creates a new JavaScript object
     */
    Scriptable newObject(Class<?> aClass, Object... arguments) {
        return context.newObject(scope, JSObject.getClassName(aClass), arguments);
    }

    /**
     * Returns an XML element that corresponds to the path. This can
     * be used when building command lines containing path to resources
     * or executables
     *
     * @return An XML element describing the path
     */
    static public Object js_path(Context cx, Scriptable thisObj, Object[] args, Function funObj) throws FileSystemException {
        if (args.length != 1)
            throw new IllegalArgumentException("path() needs one argument");

        XPMObject xpm = getXPM(thisObj);

        if (args[0] instanceof JSFileObject)
            return args[0];

        final Object o = JSUtils.unwrap(args[0]);

        if (o instanceof JSFileObject)
            return o;

        if (o instanceof FileObject)
            return xpm.newObject(JSFileObject.class, xpm, o);

        if (o instanceof String)
            return xpm.newObject(JSFileObject.class, xpm, xpm.currentResourceLocator.resolvePath(o.toString(), true).getFile());

        throw new ExperimaestroRuntimeException("Cannot convert type [%s] to a file path", o.getClass().toString());
    }

    static public String js_format(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        if (args.length == 0)
            return "";

        Object fargs[] = new Object[args.length - 1];
        for (int i = 1; i < args.length; i++)
            fargs[i - 1] = JSUtils.unwrap(args[i]);
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
            if (args.length != 1)
                throw new IllegalArgumentException("value() needs one argument");
        final Object object = JSUtils.unwrap(args[0]);

        Document doc = XMLUtils.newDocument();
        XPMObject xpm = getXPM(thisObj);
        return JSUtils.domToE4X(doc.createElement(JSUtils.toString(object)), xpm.context, xpm.scope);

    }


    /**
     * Returns the current script location
     *
     * @todo Should return a wrapper to FileObject for enhanced security
     */
    static public JSFileObject js_script_file(Context cx, Scriptable thisObj, Object[] args, Function funObj)
            throws FileSystemException {
        if (args.length != 0)
            throw new IllegalArgumentException("script_file() has no argument");

        XPMObject xpm = getXPM(thisObj);

        return new JSFileObject(xpm, xpm.currentResourceLocator.getFile());
    }

    @JSHelp(value = "Returns a file relative to the current connector")
    public static Scriptable js_file(Context cx, Scriptable thisObj, Object[] args, Function funObj) throws FileSystemException {
        XPMObject xpm = getXPM(thisObj);
        if (args.length != 1)
            throw new IllegalArgumentException("file() takes only one argument");
        final String arg = JSUtils.toString(args[0]);
        return xpm.context.newObject(xpm.scope, JSFileObject.JSCLASSNAME,
                new Object[]{xpm, xpm.currentResourceLocator.resolvePath(arg).getFile()});
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
        // First unwrap the object
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

        throw new ExperimaestroRuntimeException("Not implemented (%s)", ns.getClass());
    }


    public static Object get(Scriptable scope, final String name) {
        Object object = scope.get(name, scope);
        if (object == null && object == Undefined.instance)
            object = null;
        else if (object instanceof Wrapper)
            object = ((Wrapper) object).unwrap();
        return object;
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
        return context.newObject(scope, "XPMTaskFactory",
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
            throw new ExperimaestroRuntimeException("Could not find a task with name [%s]", qname);
        LOGGER.info("Creating a new JS task [%s]", factory.getId());
        return context.newObject(scope, "XPMTask",
                new Object[]{Context.javaToJS(factory.create(), scope)});
    }

    /**
     * Runs an XPath
     *
     * @param path
     * @param xml
     * @return
     * @throws javax.xml.xpath.XPathExpressionException
     *
     */
    static public Object js_xpath(String path, Object xml)
            throws XPathExpressionException {
        Node dom = (Node) JSUtils.toDOM(xml);
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


    /**
     * Simple evaluation of shell commands (does not create a job)
     *
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public NativeArray evaluate(Object jsargs) throws Exception {
        CommandArguments arguments = getCommandArguments(jsargs, null);

        // Run the process and captures the output
        final SingleHostConnector connector = currentResourceLocator.getConnector().getConnector(null);
        XPMProcessBuilder builder = connector.processBuilder();
        builder.command(arguments.toStrings(connector, null));
        builder.detach(false);
        builder.redirectOutput(XPMProcessBuilder.Redirect.PIPE);
        XPMProcess p = builder.start();
        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

        int len = 0;
        char[] buffer = new char[8192];
        StringBuffer sb = new StringBuffer();
        while ((len = input.read(buffer, 0, buffer.length)) >= 0)
            sb.append(buffer, 0, len);
        input.close();

        int error = p.waitFor();
        return new NativeArray(new Object[]{error, sb.toString()});
    }


    /**
     * Log a message to be returned to the client
     */
    public void log(String format, Object... objects) {
        rootLogger.info(format, objects);
        LOGGER.debug(format, objects);
    }


    /**
     * Get a QName
     */
    public QName qName(String namespaceURI, String localPart) {
        return new QName(namespaceURI, localPart);
    }

    /**
     * Get experimaestro namespace
     */
    public String ns() {
        return Manager.EXPERIMAESTRO_NS;
    }

    // XML Utilities

    public Object domToE4X(Node node) {
        return JSUtils.domToE4X(node, context, scope);
    }

    public String xmlToString(Node node) {
        return XMLUtils.toString(node);
    }


    /**
     * Creates a new command line job
     *
     * @param path      The identifier for this job
     * @param jsargs    The command line
     * @param jsoptions The options
     * @return
     * @throws Exception
     */
    public Resource commandlineJob(Object path, Object jsargs, Object jsoptions) throws Exception {
        CommandLineTask task = null;
        // --- XPMProcess arguments: convert the javascript array into a Java array
        // of String
        LOGGER.debug("Adding command line job");

        Map<String, String> pFiles = new TreeMap<>();
        final CommandArguments command = getCommandArguments(jsargs, pFiles);

        NativeObject options = jsoptions instanceof Undefined ? null : (NativeObject) jsoptions;

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
        task = new CommandLineTask(scheduler, locator, command);

        if (!submittedJobs.add(task.getLocator())) {
            LOGGER.info("Not submitting %s [duplicate]", task.getLocator());
            return scheduler.getResource(task.getLocator());
        }

        for (Map.Entry<String, String> entry : pFiles.entrySet())
            task.setParameterFile(entry.getKey(), entry.getValue());

        // -- Adds default locks
        for (Map.Entry<Resource, Object> lock : defaultLocks.entrySet()) {
            lock.getKey().createDependency(lock.getValue());
        }

        // --- Options

        if (!(jsoptions instanceof Undefined)) {
            // --- XPMProcess launcher
            if (options != null) {
                if (options.has("launcher", options)) {
                    final Object launcher = options.get("launcher", options);
                    if (launcher != null && !(launcher instanceof UniqueTag))
                        task.setLauncher(((JSLauncher) launcher).getLauncher());
                }
            }

            // --- Redirect standard output
            if (options.has("stdin", options)) {
                final Object stdin = JSUtils.unwrap(options.get("stdin", options));
                if (stdin instanceof String || stdin instanceof ConsString) {
                    task.setInput(stdin.toString());
                } else throw new ExperimaestroRuntimeException("Unsupported stdin type [%s]", stdin.getClass());
            }

            // --- Redirect standard output
            if (options.has("stdout", options)) {
                FileObject fileObject = getFileObject(connector, JSUtils.unwrap(options.get("stdout", options)));
                task.setOutput(fileObject);
            }

            // --- Redirect standard error
            if (options.has("stderr", options)) {
                FileObject fileObject = getFileObject(connector, JSUtils.unwrap(options.get("stderr", options)));
                task.setError(fileObject);
            }


            // --- Resources to lock
            if (options.has("lock", options)) {
                NativeArray resources = (NativeArray) options.get("lock", options);
                for (int i = (int) resources.getLength(); --i >= 0; ) {
                    NativeArray array = (NativeArray) resources.get(i, resources);
                    assert array.getLength() == 3;
//                    final String connectorId = Context.toString(array.get(0, array));
                    final String rsrcPath = Context.toString(array.get(0, array));

                    Resource resource = scheduler.getResource(ResourceLocator.parse(rsrcPath));

                    final Object o = array.get(1, array);
                    LOGGER.debug("Adding dependency on [%s] of type [%s]", resource, o);
                    if (resource == null)
                        throw new ExperimaestroRuntimeException("Resource [%s] was not found", rsrcPath);
                    final Dependency dependency = resource.createDependency(o);
                    task.addDependency(dependency);
                }

            }

        }

        // Update the task status now that it is initialized
        task.setGroup(defaultGroup);

        final Resource old = scheduler.getResource(task.getLocator());
        if (old != null) {
            // TODO: if equal, do not try to replace the task
            if (!task.replace(old)) {
                getRootLogger().warn(String.format("Cannot override resource [%s]", task.getIdentifier()));
                return old;
            } else {
                getRootLogger().info(String.format("Overwrote resource [%s]", task.getIdentifier()));
            }
        } else {
            scheduler.store(task, false);
        }

        return task;
    }

    private static FileObject getFileObject(Connector connector, Object stdout) throws FileSystemException {
        if (stdout instanceof String || stdout instanceof ConsString)
            return connector.getMainConnector().resolveFile(stdout.toString());

        if (stdout instanceof JSFileObject)
            return connector.getMainConnector().resolveFile(stdout.toString());

        if (stdout instanceof FileObject)
            return (FileObject) stdout;

        throw new ExperimaestroRuntimeException("Unsupported stdout type [%s]", stdout.getClass());
    }

    /**
     * Transform an array of JS objects into a command line argument object
     *
     * @param jsargs         The input array
     * @param parameterFiles (out) A map that will contain the parameter files defined in the command line
     * @return a valid {@linkplain CommandArgument} object
     */
    private static CommandArguments getCommandArguments(Object jsargs, Map<String, String> parameterFiles) {
        final CommandArguments command = new CommandArguments();

        if (jsargs instanceof NativeArray) {
            NativeArray array = ((NativeArray) jsargs);

            for (Object _object : array) {
                final CommandArgument argument = new CommandArgument();
                Object object = JSUtils.unwrap(_object);
                StringBuilder sb = new StringBuilder();

                // XML argument (deprecated -- too many problems with E4X!)
                if (JSUtils.isXML(object)) {

                    // Walk through
                    for (Node child : xmlAsList(JSUtils.toDOM(object)))
                        argumentWalkThrough(sb, argument, child);

                } else {
                    argumentWalkThrough(sb, argument, object, parameterFiles);
                }

                if (sb.length() > 0)
                    argument.add(sb.toString());

                command.add(argument);
            }

        } else
            throw new RuntimeException(format(
                    "Cannot handle an array of type %s", jsargs.getClass()));
        return command;
    }

    /**
     * Recursive parsing of the command line
     */
    private static void argumentWalkThrough(StringBuilder sb, CommandArgument argument, Object object,
                                            Map<String, String> parameterFiles) {
        if (object instanceof JSFileObject)
            object = ((JSFileObject) object).getFile();

        if (object instanceof FileObject) {
            if (sb.length() > 0) {
                argument.add(sb.toString());
                sb.delete(0, sb.length());
            }
            argument.add(new CommandArgument.Path((FileObject) object));
        } else if (object instanceof NativeArray) {
            for (Object child : (NativeArray) object)
                argumentWalkThrough(sb, argument, JSUtils.unwrap(child), parameterFiles);
        } else if (JSUtils.isXML(object)) {
            final Object node = JSUtils.toDOM(object);
            for (Node child : xmlAsList(node))
                argumentWalkThrough(sb, argument, child);
        } else if (object instanceof JSParameterFile) {
            final JSParameterFile pFile = (JSParameterFile) object;
            argument.add(new CommandArgument.ParameterFile(pFile.key));
            parameterFiles.put(pFile.key, pFile.value);

        } else {
            sb.append(JSUtils.toString(object));
        }
    }

    /**
     * Walk through a node hierarchy to build a command argument
     *
     * @param sb
     * @param argument
     * @param node
     */
    private static void argumentWalkThrough(StringBuilder sb, CommandArgument argument, Node node) {
        switch (node.getNodeType()) {
            case Node.TEXT_NODE:
                sb.append(node.getTextContent());
                break;

            case Node.ATTRIBUTE_NODE:
                if (Manager.XP_PATH.sameQName(node)) {
                    argument.add(new CommandArgument.Path(node.getNodeValue()));
                } else
                    sb.append(node.getTextContent());
                break;

            case Node.ELEMENT_NODE:
                Element element = (Element) node;
                if (XMLUtils.is(Manager.XP_PATH, element)) {
                    if (sb.length() > 0) {
                        argument.add(sb.toString());
                        sb.delete(0, sb.length());
                    }
                    argument.add(new CommandArgument.Path(element.getTextContent()));
                } else {
                    for (Node child : XMLUtils.children(node))
                        argumentWalkThrough(sb, argument, child);
                }

                break;
            default:
                throw new ExperimaestroRuntimeException("Unhandled command XML node  " + node.toString());
        }

    }

    /**
     * Transforms an XML related object into a list
     *
     * @param object
     * @return
     */
    private static Iterable<? extends Node> xmlAsList(Object object) {

        if (object instanceof Node) {
            Node node = (Node) object;
            return (node.getNodeType() == Node.ELEMENT_NODE && node.getChildNodes().getLength() > 1)
                    || node.getNodeType() == Node.DOCUMENT_FRAGMENT_NODE ?

                    XMLUtils.children(node) : Arrays.asList(node);
        }

        if (object instanceof NodeList)
            return XMLUtils.iterable((NodeList) object);

        throw new AssertionError("Cannot handle object of type " + object.getClass());
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

    public void setLocator(ResourceLocator locator) {
        this.currentResourceLocator = locator;
    }


    // --- Javascript methods

    static public class JSXPM extends ScriptableObject {
        XPMObject xpm;

        public JSXPM() {
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
            final Object x = JSUtils.unwrap(object);
            xpm.properties.put(name, object);
        }

        @JSFunction("set_default_group")
        public void setDefaultGroup(String name) {
            xpm.defaultGroup = name;
        }

        @JSFunction("set_default_lock")
        @JSHelp("Adds a new resource to lock for all jobs to be started")
        public void setDefaultLock(Object resource, Object parameters) {
            xpm.defaultLocks.put((Resource) JSUtils.unwrap(resource), parameters);
        }

        @JSFunction("token_resource")
        @JSHelp("Retrieve (or creates) a token resource with a given path")
        public Scriptable getTokenResource(@JSArgument(name = "path", help = "The path of the resource") String path) throws ExperimaestroCannotOverwrite {
            final ResourceLocator locator = new ResourceLocator(XPMConnector.ID, path);
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


        static public void log(Level level, Context cx, Scriptable thisObj, Object[] args, Function funObj) {
            if (args.length < 1)
                throw new ExperimaestroRuntimeException("There should be at least one argument for log()");

            String format = Context.toString(args[0]);
            Object[] objects = new Object[args.length - 1];
            for (int i = 1; i < args.length; i++)
                objects[i - 1] = JSUtils.unwrap(args[i]);

            ((JSXPM) thisObj).xpm.log(format, objects);
        }

        @JSFunction("log")
        static public void log(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
            log(Level.INFO, cx, thisObj, args, funObj);
        }

        @JSFunction("logger")
        public Scriptable getLogger(String name) {
            return xpm.newObject(JSLogger.class, xpm, name);
        }


        @JSFunction("log_level")
        @JSHelp(value = "Sets the logger debug level")
        public void setLogLevel(@JSArgument(name = "name") String name, @JSArgument(type = "String", name = "level") String level) {
            Logger.getLogger(xpm.loggerRepository, name).setLevel(Level.toLevel(level));
        }


        @JSFunction("get_script_path")
        public String getScriptPath() {
            return xpm.currentResourceLocator.getPath();
        }

        @JSFunction("get_script_file")
        static public Scriptable getScriptFile(Context cx, Scriptable thisObj, Object[] args, Function funObj) throws FileSystemException {
            if (args.length != 0)
                throw new IllegalArgumentException("xpm.get_script_file() takes no argument");

            final XPMObject xpm = ((JSXPM) thisObj).xpm;
            return xpm.newObject(JSFileObject.class, xpm, xpm.currentResourceLocator.getFile());
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
        public Scriptable add_task_factory(NativeObject object) {
            JSTaskFactory f = new JSTaskFactory(xpm.scope, object, xpm.repository);
            xpm.repository.addFactory(f);
            return xpm.context.newObject(xpm.scope, "XPMTaskFactory",
                    new Object[]{Context.javaToJS(f, xpm.scope)});
        }


        @JSFunction("get_task")
        static public Scriptable getTask(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
            final XPMObject xpm = ((JSXPM) thisObj).xpm;
            if (args.length == 1)
                return xpm.getTask((QName) JSUtils.unwrap(args[0]));

            if (args.length == 2)
                return xpm.getTask(Context.toString(args[0]), Context.toString(args[1]));

            throw new IllegalArgumentException("get_task() called with the wrong number of arguments");
        }


        @JSFunction("evaluate")
        public NativeArray evaluate(Object jsargs) throws Exception {
            return xpm.evaluate(jsargs);
        }

        @JSFunction("file")
        @JSHelp(value = "Returns a file relative to the current connector")
        public Scriptable file(@JSArgument(name = "filepath") String filepath) throws FileSystemException {
            return xpm.context.newObject(xpm.scope, JSFileObject.JSCLASSNAME,
                    new Object[]{xpm, xpm.currentResourceLocator.resolvePath(filepath).getFile()});
        }


        @JSFunction("command_line_job")
        @JSHelp(value = "Schedule a command line job")
        public Scriptable commandlineJob(@JSArgument(type = "String", name = "jobId") Object path,
                                         @JSArgument(type = "Array", name = "command") Object jsargs,
                                         @JSArgument(type = "Map", name = "options") Object jsoptions) throws Exception {
            return xpm.newObject(JSResource.class, xpm.commandlineJob(JSUtils.unwrap(path), jsargs, jsoptions));
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
            final Iterable<? extends Node> list = XPMObject.xmlAsList(JSUtils.toDOM(xml));
            for (Node node : list) {
                output(node);
            }
        }

        @JSFunction("publish")
        @JSHelp("Publish the repository on the web server")
        public void publish() throws InterruptedException {
            TasksServlet.updateRepository(xpm.currentResourceLocator.toString(), xpm.repository);
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
}