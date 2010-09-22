package bpiwowar.expmanager.experiments;

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.UniqueTag;

import bpiwowar.expmanager.rsrc.CommandLineTask;
import bpiwowar.expmanager.rsrc.TaskManager;
import bpiwowar.log.Logger;

/**
 * Handle javascript calls
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class JSHandler {
	final static private Logger LOGGER = Logger.getLogger();

	/**
	 * The experiment repository
	 */
	private final Repository repository;

	/**
	 * Our scope
	 */
	private final Scriptable scope;

	private Context context;

	private final TaskManager manager;

	public JSHandler(Context cx, Scriptable scope, Repository repository,
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
	public void addExperiment(NativeObject object) {
		repository.register(new JSInformation(context, scope, object));
	}

	/**
	 * Get the documentation of an experiment
	 * 
	 * @param id
	 * @return
	 */
	public String getDocumentation(String id) {
		Information information = repository.get(id);
		if (information == null)
			return null;

		return information.getDocumentation();
	}

	/**
	 * Get the documentation of an experiment
	 * 
	 * @param id
	 * @return
	 */
	public Information getExperiment(String id) {
		Information information = repository.get(id);
		return information;
	}

	/**
	 * Run a command line experiment
	 * 
	 * @return
	 */
	public void addCommandLineJob(String identifier, Object jsargs) {
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

		// Add it
		CommandLineTask task = new CommandLineTask(manager, identifier, args);
		manager.add(task);
	}

	/**
	 * Captures a bash script
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

	/**
	 * @param object
	 * @return
	 */
	@SuppressWarnings("unchecked")
	static <T> T get(Scriptable scope, String name, NativeObject object) {
		final Object _value = object.get(name, scope);
		if (_value == UniqueTag.NOT_FOUND)
			throw new RuntimeException(format("Could not find property '%s'",
					name));
		return (T) _value;
	}

}
