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

import com.sleepycat.je.DatabaseException;
import com.sun.org.apache.xerces.internal.impl.xs.XSLoaderImpl;
import com.sun.org.apache.xerces.internal.xs.XSModel;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.mozilla.javascript.*;
import org.mozilla.javascript.annotations.JSFunction;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.ls.LSInput;
import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.connectors.SSHOptions;
import sf.net.experimaestro.connectors.XPMProcess;
import sf.net.experimaestro.connectors.XPMProcessBuilder;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.manager.*;
import sf.net.experimaestro.plan.ParseException;
import sf.net.experimaestro.plan.PlanParser;
import sf.net.experimaestro.scheduler.LockMode;
import sf.net.experimaestro.scheduler.ResourceLocator;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.scheduler.SimpleData;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.Output;
import sf.net.experimaestro.utils.XMLUtils;
import sf.net.experimaestro.utils.log.Logger;

import javax.xml.xpath.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
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
     * Properties set by the script
     */
    Map<String, Object> properties = new HashMap<>();

    private static ThreadLocal<ArrayList<String>> log = new ThreadLocal<ArrayList<String>>() {
        protected synchronized ArrayList<String> initialValue() {
            return new ArrayList<String>();
        }
    };

    /**
     * Initialise a new XPM object
     *
     * @param currentResourceLocator The path to the current script
     * @param cx
     * @param environment
     * @param scope
     * @param repository
     * @param scheduler
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws InvocationTargetException
     * @throws SecurityException
     * @throws NoSuchMethodException
     */
    public XPMObject(ResourceLocator currentResourceLocator, Context cx, Map<String, String> environment,
                     Scriptable scope, Repository repository, Scheduler scheduler)
            throws IllegalAccessException, InstantiationException,
            InvocationTargetException, SecurityException, NoSuchMethodException {
        LOGGER.info("Current script is %s", currentResourceLocator);
        this.currentResourceLocator = currentResourceLocator;
        this.context = cx;
        this.environment = environment;
        this.scope = scope;
        this.repository = repository;
        this.scheduler = scheduler;

        // --- Define functions and classes

        // Define the new classes
        ScriptableObject.defineClass(scope, TaskFactoryJSWrapper.class);
        ScriptableObject.defineClass(scope, TaskJSWrapper.class);
        ScriptableObject.defineClass(scope, JSScheduler.class);
        ScriptableObject.defineClass(scope, JSConnector.class);
        ScriptableObject.defineClass(scope, SSHOptions.class);
        ScriptableObject.defineClass(scope, JSObject.class);

        // Launchers
        ScriptableObject.defineClass(scope, JSOARLauncher.class);

        // ComputationalResources

        // Add functions
        JSUtils.addFunction(XPMObject.class, scope, "qname", new Class<?>[]{Object.class, String.class});
        JSUtils.addFunction(XPMObject.class, scope, "include_repository", null);
        JSUtils.addFunction(XPMObject.class, scope, "script_file", null);
        JSUtils.addFunction(XPMObject.class, scope, "include", null);

        // Adds some special functions available for tests only
        JSUtils.addFunction(SSHServer.class, scope, "sshd_server", new Class[]{});


        // TODO: would be good to have this at a global level

        // --- Add new objects

//        ScriptableObject.defineProperty(scope, "xpm", new JSObject(this), 0);
        addNewObject(cx, scope, "xpm", "XPM", new Object[]{});
        ((JSObject) get(scope, "xpm")).set(this);

        addNewObject(cx, scope, "xp", "Namespace", new Object[]{"xp",
                Manager.EXPERIMAESTRO_NS});
        addNewObject(cx, scope, "scheduler", "Scheduler",
                new Object[]{scheduler});
    }

    static private void addNewObject(Context cx, Scriptable scope,
                                     final String objectName, final String className,
                                     final Object[] params) {
        ScriptableObject.defineProperty(scope, objectName,
                cx.newObject(scope, className, params), 0);
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
            throws Exception, InstantiationException, IllegalAccessException {
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
    public XPMObject include(String path, boolean repositoryMode) throws Exception, IllegalAccessException, InstantiationException {
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
                xpmObject = new XPMObject(scriptpath, context, newEnvironment, scriptScope, repository, scheduler);

            }

            Context.getCurrentContext().evaluateReader(scriptScope, new InputStreamReader(inputStream), scriptpath.toString(), 1, null);

            return xpmObject;
        }

    }


    static XPMObject include(Context cx, Scriptable thisObj, Object[] args,
                             Function funObj, boolean repositoryMode) throws Exception {
        XPMObject xpm = ((JSObject) thisObj.get("xpm", thisObj)).xpm;

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
     * Returns the current script location
     *
     * @todo Should return a wrapper to FileObject for enhanced security
     */
    static public FileObject js_script_file(Context cx, Scriptable thisObj, Object[] args, Function funObj)
            throws FileSystemException {
        if (args.length != 0)
            throw new IllegalArgumentException("script_file() has no argument");

        XPMObject xpm = (XPMObject) thisObj.get("xpm", thisObj);

        return xpm.currentResourceLocator.getFile();
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
        LOGGER.info("Creating a new JS task factory %s", factory);
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
        LOGGER.info("Creating a new JS task %s", factory);
        return context.newObject(scope, "XPMTask",
                new Object[]{Context.javaToJS(factory.create(), scope)});
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

    public String addData(Connector connector, String identifier) throws DatabaseException {
        LockMode mode = LockMode.SINGLE_WRITER;
        SimpleData resource = new SimpleData(scheduler, connector, identifier, mode, false);
        scheduler.store(resource);
        return identifier;
    }

    /**
     * Simple evaluation of shell commands (does not create a job)
     *
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public NativeArray evaluate(Object jsargs) throws Exception {
        final String[] args;
        if (jsargs instanceof NativeArray) {
            NativeArray array = ((NativeArray) jsargs);
            int length = (int) array.getLength();
            args = new String[length];
            for (int i = 0; i < length; i++) {
                Object el = array.get(i, array);
                if (el instanceof NativeJavaObject)
                    el = ((NativeJavaObject) el).unwrap();
                LOGGER.debug("arg %d: %s/%s", i, el, el.getClass());
                args[i] = el.toString();
            }
        } else
            throw new RuntimeException(format(
                    "Cannot handle an array of type %s", jsargs.getClass()));

        // Run the process and captures the output
        XPMProcessBuilder builder = currentResourceLocator.getConnector().getConnector(null).processBuilder();
        builder.command(args);
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
        String msg = format(format, objects);
        log.get().add(msg);
        LOGGER.debug(msg);
    }

    /**
     * Get the log for the current thread
     *
     * @return
     */
    static public ArrayList<String> getLog() {
        return log.get();
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

    public static void resetLog() {
        log.set(new ArrayList<String>());
    }




    /**
     * Execute an experimental plan
     *
     * @throws ParseException If the plan is not readable
     */
    public Object experiment(QName qname, String planString)
            throws ParseException {
        // Get the task
        TaskFactory taskFactory = repository.getFactory(qname);
        if (taskFactory == null)
            throw new ExperimaestroRuntimeException("No task factory with id [%s]",
                    qname);

        // Parse the plan

        PlanParser planParser = new PlanParser(new StringReader(planString));
        sf.net.experimaestro.plan.Node plans = planParser.plan();
        LOGGER.info("Plan is %s", plans.toString());
        for (Map<String, String> plan : plans) {
            // Run a plan
            LOGGER.info("Running plan: %s",
                    Output.toString(" * ", plan.entrySet()));
            Task task = taskFactory.create();
            for (Map.Entry<String, String> kv : plan.entrySet())
                task.setParameter(DotName.parse(kv.getKey()), kv.getValue());
            task.run();
        }
        return null;
    }

    /**
     * Runs an XPath
     *
     * @param path
     * @param xml
     * @return
     * @throws XPathExpressionException
     */
    public Object xpath(String path, Object xml)
            throws XPathExpressionException {
        Node dom = JSUtils.toDOM(xml);
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
     * Add an XML Schema declaration
     *
     * @param module
     * @param path
     */
    public void addSchema(Object module, final String path) throws IOException {
        LOGGER.info("Loading XSD file [%s], with script path [%s]", path, currentResourceLocator.toString());
        ResourceLocator file = currentResourceLocator.resolvePath(path, true);
        XSLoaderImpl xsLoader = new XSLoaderImpl();
        XSModel xsModel = null;

        xsModel = xsLoader.load(new LSInput() {
            @Override
            public Reader getCharacterStream() {
                return null;
            }

            @Override
            public void setCharacterStream(Reader reader) {
                throw new AssertionError("Should not be called");
            }

            @Override
            public InputStream getByteStream() {
                try {
                    return currentResourceLocator.resolvePath(path, true).getFile().getContent().getInputStream();
                } catch (Exception e) {
                    throw new ExperimaestroRuntimeException(e);
                }
            }

            @Override
            public void setByteStream(InputStream inputStream) {
                throw new AssertionError("Should not be called");
            }

            @Override
            public String getStringData() {
                return null;
            }

            @Override
            public void setStringData(String s) {
                throw new AssertionError("Should not be called");
            }

            @Override
            public String getSystemId() {
                return null;
            }

            @Override
            public void setSystemId(String s) {
                throw new AssertionError("Should not be called");
            }

            @Override
            public String getPublicId() {
                return null;
            }

            @Override
            public void setPublicId(String s) {
                throw new AssertionError("Should not be called");
            }

            @Override
            public String getBaseURI() {
                return null;
            }

            @Override
            public void setBaseURI(String s) {
                throw new AssertionError("Should not be called");
            }

            @Override
            public String getEncoding() {
                return null;
            }

            @Override
            public void setEncoding(String s) {
                throw new AssertionError("Should not be called");
            }

            @Override
            public boolean getCertifiedText() {
                return false;
            }

            @Override
            public void setCertifiedText(boolean b) {
                throw new AssertionError("Should not be called");
            }
        });

        // Add to the repository
        repository.addSchema(JSModule.getModule(repository, module), xsModel);
    }


    // --- Javascript methods

    static public class JSObject extends ScriptableObject {
        XPMObject xpm;

        public JSObject() {
            LOGGER.info("Called WITHOUT xpm");
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

        @JSFunction("log")
        static public void log(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
            if (args.length < 1)
                throw new ExperimaestroRuntimeException("There should be at least one argument for log()");

            String format = Context.toString(args[0]);
            Object[] objects = new Object[args.length - 1];
            for (int i = 1; i < args.length; i++)
                objects[i - 1] = JSUtils.unwrap(args[i]);

            ((JSObject) thisObj).xpm.log(format, objects);
        }

        @JSFunction("get_script_path")
        public String getScriptPath() {
            return xpm.currentResourceLocator.getPath();
        }

        /**
         * Add a module
         */
        @JSFunction("add_module")
        public void addModule(Object object) {
            JSModule module = new JSModule(xpm.repository, xpm.scope, (NativeObject) object);
            LOGGER.debug("Adding module [%s]", module.getId());
            xpm.repository.addModule(module);
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
            final XPMObject xpm = ((JSObject) thisObj).xpm;
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

        static public File filepath(File file, String... names) {
            for (String name : names)
                file = new File(file, name);
            return file;
        }

        @JSFunction("filepath")
        static public File filepath(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
            if (args.length < 1)
                throw new IllegalArgumentException("filepath() called with the wrong number of arguments");

            String[] names = new String[args.length - 1];
            for (int i = 0; i < names.length; i++)
                names[i] = Context.toString(args[i + 1]);

            final Object o = JSUtils.unwrap(args[0]);
            if (o instanceof File)
                return filepath((File) o, names);

            return filepath(new File(Context.toString(args[0])));
        }

        /**
         * Declare an alternative
         */
        @JSFunction("declare_alternative")
        public void declareAlternative(Object qname) {
            AlternativeType type = new AlternativeType((QName)qname);
            xpm.repository.addType(type);
        }


    }
}