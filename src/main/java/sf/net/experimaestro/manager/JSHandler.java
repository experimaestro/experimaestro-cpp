package sf.net.experimaestro.manager;

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import sf.net.experimaestro.rsrc.CommandLineTask;
import sf.net.experimaestro.rsrc.TaskManager;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.log.Logger;


/**
 * This class contains both utility static methods and functions that can be
 * called from javascript
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class JSHandler {
	final static private Logger LOGGER = Logger.getLogger();

	/**
	 * The experiment repository
	 */
	private final TaskRepository repository;

	/**
	 * Our scope (global among javascripts)
	 */
	private final Scriptable scope;

	/**
	 * The context (local)
	 */
	private Context context;

	private final TaskManager manager;

	public JSHandler(Context cx, Scriptable scope, TaskRepository repository,
			TaskManager manager) {
		this.context = cx;
		this.scope = scope;
		this.repository = repository;
		this.manager = manager;
	}

	/**
	 * Add an experiment
	 * 
	 * @param object
	 */
	public void addTask(NativeObject object) {
		repository.register(new JSTaskInformation(context, scope, object));
	}

	/**
	 * Get the documentation for a task of a given id
	 * 
	 * @param id
	 * @return An XHTML string or null if the task does not exist
	 */
	public String getDocumentation(String id) {
		TaskInformation information = repository.get(id);
		if (information == null)
			return null;

		return information.getDocumentation();
	}

	/**
	 * Get the information about a given task
	 * 
	 * @param id
	 * @return
	 */
	public TaskInformation getExperiment(String id) {
		TaskInformation information = repository.get(id);
		return information;
	}

	
	/**
	 * Run a command line experiment
	 * @param jsargs a native array
	 * @param a E4X object 
	 * @return
	 */
	public void addCommandLineJob(String identifier, Object jsargs,
			Object jsresources) {
		// --- Process arguments: convert the javascript array into a Java array
		// of String
		final String[] args;
		if (jsargs instanceof NativeArray) {
			NativeArray array = ((NativeArray) jsargs);
			int length = (int) array.getLength();
			args = new String[length];
			for (int i = 0; i < length; i++) {
				Object el = array.get(i, array);
				LOGGER.debug("arg %d: %s/%s", i, el, el.getClass());
				args[i] = el.toString();
			}
		} else
			throw new RuntimeException(format(
					"Cannot handle an array of type %s", jsargs.getClass()));
		
		// --- Process the resources
		Node resources = JSUtils.toDOM(jsresources);
		NodeList children = resources.getChildNodes();
		
		
		
		// --- Add it
		CommandLineTask task = new CommandLineTask(manager, identifier, args);
		manager.add(task);
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

}
