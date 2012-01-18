/*
 *
 *  This file is part of experimaestro.
 *  Copyright (c) 2011 B. Piwowarski <benjamin@bpiwowar.net>
 *
 *  experimaestro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  experimaestro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package sf.net.experimaestro.manager.js;

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFunctionResolver;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import sf.net.experimaestro.exceptions.ExperimaestroException;
import sf.net.experimaestro.manager.AlternativeType;
import sf.net.experimaestro.manager.DotName;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.NSContext;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.manager.XPMXPathFunctionResolver;
import sf.net.experimaestro.plan.ParseException;
import sf.net.experimaestro.plan.PlanParser;
import sf.net.experimaestro.scheduler.LockMode;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.scheduler.SimpleData;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.Output;
import sf.net.experimaestro.utils.XMLUtils;
import sf.net.experimaestro.utils.log.Logger;

import com.sleepycat.je.DatabaseException;
import com.sun.org.apache.xerces.internal.impl.xs.XSLoaderImpl;
import com.sun.org.apache.xerces.internal.xs.XSModel;

/**
 * This class contains both utility static methods and functions that can be
 * called from javascript
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
/**
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 *
 */
public class XPMObject {

	final static private Logger LOGGER = Logger.getLogger();

	/**
	 * The environment variable that contains the script path
	 */
	public static final String ENV_SCRIPTPATH = "XPM_JS_SCRIPTPATH";

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

	private final Scheduler scheduler;

	private final Map<String, String> environment;

	private static ThreadLocal<ArrayList<String>> log = new ThreadLocal<ArrayList<String>>() {
		protected synchronized ArrayList<String> initialValue() {
			return new ArrayList<String>();
		}
	};

	public XPMObject(Context cx, Map<String, String> environment,
			Scriptable scope, Repository repository, Scheduler scheduler)
			throws IllegalAccessException, InstantiationException,
			InvocationTargetException, SecurityException, NoSuchMethodException {
		this.context = cx;
		this.environment = environment;
		this.scope = scope;
		this.repository = repository;
		this.scheduler = scheduler;

		// Define the new classes
		ScriptableObject.defineClass(scope, TaskFactoryJSWrapper.class);
		ScriptableObject.defineClass(scope, TaskJSWrapper.class);
		ScriptableObject.defineClass(scope, JSScheduler.class);

		// Add functions
		addFunction(scope, "qname",
				new Class<?>[] { Object.class, String.class });

        // TODO: would be good to have this at a global level
		//addFunction(scope, "include", new Class<?>[] { String.class });

		// Add this object
		ScriptableObject.defineProperty(scope, "xpm", this, 0);

		// Add new objects
		addNewObject(cx, scope, "xp", "Namespace", new Object[] { "xp",
				Manager.EXPERIMAESTRO_NS });
		addNewObject(cx, scope, "scheduler", "Scheduler",
				new Object[] { scheduler });
	}

	static private void addNewObject(Context cx, Scriptable scope,
			final String objectName, final String className,
			final Object[] params) {
		ScriptableObject.defineProperty(scope, objectName,
				cx.newObject(scope, className, params), 0);
	}

	/**
	 * Add a new javascript function to the scope
	 * 
	 * @param scope
	 * @param fname
	 * @param prototype
	 * @throws NoSuchMethodException
	 */
	static private void addFunction(Scriptable scope, final String fname,
			final Class<?>[] prototype) throws NoSuchMethodException {
		final FunctionObject f = new FunctionObject(fname,
				XPMObject.class.getMethod("js_" + fname, prototype), scope);
		ScriptableObject.putProperty(scope, fname, f);
	}

	/**
	 * Returns a QName object
	 * 
	 * @param ns
	 *            The namespace: can be the URI string, or a javascript
	 *            Namespace object
	 * @param localName
	 *            the localname
	 * @return a QName object
	 */
	static public Object js_qname(Object ns, String localName) {
		if (ns instanceof Wrapper)
			ns = ((Wrapper) ns).unwrap();
		if (ns instanceof ScriptableObject) {
			ScriptableObject scriptableObject = (ScriptableObject) ns;
			if (scriptableObject.getClassName().equals("Namespace")) {
				Object object = scriptableObject.get("uri", null);
				return new QName(object.toString(), localName);
			}
		}

		if (ns instanceof String)
			return new QName((String) ns, localName);

		throw new ExperimaestroException("Not implemented (%s)", ns.getClass());
	}

	/**
	 * Add an experiment
	 * 
	 * @param object
	 * @return
	 */
	public Scriptable addTaskFactory(NativeObject object) {
		JSTaskFactory f = new JSTaskFactory(scope, object, repository);
		repository.addFactory(f);
		return context.newObject(scope, "XPMTaskFactory",
				new Object[] { Context.javaToJS(f, scope) });
	}

	/**
	 * Include a javascript
	 * 
	 * @param path
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public void include(String path) throws FileNotFoundException,
			IOException {

        String oldPath = environment.get(ENV_SCRIPTPATH);
        
        File file = getAbsoluteFile(path);

		LOGGER.debug("Including file [%s]", file);
		environment.put(ENV_SCRIPTPATH, file.getAbsolutePath());

		// Run JS
		Context.getCurrentContext().evaluateReader(scope, new FileReader(file),
				file.getAbsolutePath(), 1, null);

		environment.put(ENV_SCRIPTPATH, oldPath);
	}

	/**
	 * Get an absolute path from a possibly relative path
	 * 
	 * @param path
	 *            The relative or absolute path
	 * @return An absolute path
	 */
	private File getAbsoluteFile(String path) {
		File file = new File(path);
		String scriptPath = null;
		if (!file.isAbsolute()) {
			scriptPath = environment.get(ENV_SCRIPTPATH);
			if (scriptPath == null)
				throw new ExperimaestroException(
						"Cannot include file [%s] since the including file is not defined",
						path);
			file = new File(new File(scriptPath).getParentFile(), path);
		}
		return file;
	}

	public static Object get(Scriptable scope, final String name) {
		Object object = scope.get(name, scope);
		if (object == null && object == Undefined.instance)
			object = null;
		else

		if (object instanceof Wrapper)
			object = ((Wrapper) object).unwrap();
		return object;
	}

	/**
	 * Get the information about a given task
	 * 
	 * @param id
	 * @return
	 */
	public Scriptable getTaskFactory(String namespace, String id) {
		TaskFactory factory = repository.getFactory(new QName(namespace, id));
		LOGGER.info("Creating a new JS task factory %s", factory);
		return context.newObject(scope, "XPMTaskFactory",
				new Object[] { Context.javaToJS(factory, scope) });
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
		LOGGER.info("Creating a new JS task %s", factory);
		return context.newObject(scope, "XPMTask",
				new Object[] { Context.javaToJS(factory.create(), scope) });
	}

	/**
	 * Returns the script path if available
	 */
	public String getScriptPath() {
		return environment.get(ENV_SCRIPTPATH);
	}

	/**
	 * Recursive flattening of an array
	 * 
	 * @param array
	 *            The array to flatten
	 * @param list
	 *            A list of strings that will be filled
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

	public String addData(String identifier) throws DatabaseException {
		LockMode mode = LockMode.SINGLE_WRITER;
		SimpleData resource = new SimpleData(scheduler, identifier, mode, false);
		scheduler.add(resource);
		return identifier;
	}

	/**
	 * Simple evaluation of shell commands
	 * 
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public NativeArray evaluate(Object jsargs) throws IOException,
			InterruptedException {
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
		Process p = Runtime.getRuntime().exec(args);
		BufferedReader input = new BufferedReader(new InputStreamReader(
				p.getInputStream()));

		int len = 0;
		char[] buffer = new char[8192];
		StringBuffer sb = new StringBuffer();
		while ((len = input.read(buffer, 0, buffer.length)) >= 0)
			sb.append(buffer, 0, len);
		input.close();

		int error = p.waitFor();
		return new NativeArray(new Object[] { error, sb.toString() });
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

	public File filepath(String filepath, String... names) {
		File file = new File(filepath);
		for (String name : names)
			file = new File(file, name);
		return file;
	}

	public File filepath(File file, String... names) {
		for (String name : names)
			file = new File(file, name);
		return file;
	}

	/** Declare an alternative */
	public void declareAlternative(QName qname) {
		AlternativeType type = new AlternativeType(qname);
		repository.addType(type);
	}

	/**
	 * Add a module
	 */
	public void addModule(Object object) {
		JSModule module = new JSModule(repository, scope, (NativeObject) object);
		LOGGER.debug("Adding module [%s]", module.getId());
		repository.addModule(module);
	}

	/**
	 * Execute an experimental plan
	 * 
	 * @throws ParseException
	 *             If the plan is not readable
	 */
	public Object experiment(QName qname, String planString)
			throws ParseException {
		// Get the task
		TaskFactory taskFactory = repository.getFactory(qname);
		if (taskFactory == null)
			throw new ExperimaestroException("No task factory with id [%s]",
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
	public void addSchema(Object module, String path) {
		LOGGER.info("Loading XSD file [%s], with script path [%s]", path,
				getScriptPath());
		File file = getAbsoluteFile(path);
		XSLoaderImpl xsLoader = new XSLoaderImpl();
		XSModel xsModel = xsLoader.loadURI(file.toURI().toString());

		// Add to the repository
		repository.addSchema(JSModule.getModule(repository, module), xsModel);
	}

}
