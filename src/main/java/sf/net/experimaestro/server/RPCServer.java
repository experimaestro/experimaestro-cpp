package sf.net.experimaestro.server;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Level;
import org.mortbay.jetty.Server;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrappedException;

import sf.net.experimaestro.locks.LockType;
import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.manager.js.XPMObject;
import sf.net.experimaestro.scheduler.CommandLineTask;
import sf.net.experimaestro.scheduler.LockMode;
import sf.net.experimaestro.scheduler.Resource;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.scheduler.SimpleData;
import sf.net.experimaestro.utils.Output;
import sf.net.experimaestro.utils.log.Logger;

import com.sleepycat.je.DatabaseException;

/**
 * Our RPC handler for experimaestro
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class RPCServer {
	final static private Logger LOGGER = Logger.getLogger();

	/**
	 * The task manager
	 */
	private Scheduler scheduler;

	/**
	 * Repository
	 */
	Repository repository;

	/**
	 * Server
	 */
	private Server server;

	/**
	 * Set the task server
	 * 
	 * @param scheduler
	 * @param repository
	 */
	public void setTaskServer(Server server, Scheduler scheduler,
			Repository repository) {
		this.server = server;
		this.scheduler = scheduler;
		this.repository = repository;
	}

	/**
	 * Shutdown the server
	 */
	public boolean shutdown() {
		// Shutdown jetty
		server.setGracefulShutdown(1000);
		boolean stopped = false;
		try {
			server.stop();
			stopped = true;
		} catch (Exception e) {
			LOGGER.error(e, "Could not stop properly jetty");
		}

		// Close the repository
		repository.close();

		// Close the scheduler
		scheduler.close();

		
		// If not OK, do force
		if (!stopped)
			System.exit(1);
		
		// Everything's OK
		return true;
	}

	/**
	 * Add a data resource
	 * 
	 * @param id
	 *            The data ID
	 * @param mode
	 *            The locking mode
	 * @param exists
	 * @return
	 * @throws DatabaseException
	 */
	public boolean addData(String id, String mode, boolean exists)
			throws DatabaseException {
		LOGGER.info("Addind data %s [%s/%b]", id, mode, exists);
		scheduler.add(new SimpleData(scheduler, id, LockMode.valueOf(mode),
				exists));
		return true;
	}

	/**
	 * A class that is used to control the environment in scripts
	 * 
	 * @author B. Piwowarski <benjamin@bpiwowar.net>
	 */
	static public class JSGetEnv {
		private final Map<String, String> environment;

		public JSGetEnv(Map<String, String> environment) {
			this.environment = environment;
		}

		public String get(String key) {
			return environment.get(key);
		}

		public String get(String key, String defaultValue) {
			String value = environment.get(key);
			if (value == null)
				return defaultValue;
			return value;
		}

	}

	/**
	 * Run a javascript script (either the file or a string)
	 * 
	 * This version is called from python scripts where maps would be marshalled
	 * into a string. Instead, we get a list that we transform into a map.
	 */
	public ArrayList<Object> runJSScript(boolean isFile, String content,
			Object[] envArray) {
		Map<String, String> environment = arrayToMap(envArray);
		return runJSScript(isFile, content, environment);
	}

	/**
	 * Run a javascript script (either the file or a string)
	 */
	public ArrayList<Object> runJSScript(boolean isFile, String content,
			Map<String, String> environment) {
		int error = 0;
		String errorMsg = "";
		XPMObject jsXPM = null;

		// Creates and enters a Context. The Context stores information
		// about the execution environment of a script.
		try {
			org.mozilla.javascript.Context jsContext = org.mozilla.javascript.Context
					.enter();

			// Initialize the standard objects (Object, Function, etc.)
			// This must be done before scripts can be executed. Returns
			// a scope object that we use in later calls.
			Scriptable scope = jsContext.initStandardObjects();

			LOGGER.debug("Environment is: %s", Output.toString(", ",
					environment.entrySet(),
					new Output.Formatter<Map.Entry<String, String>>() {
						@Override
						public String format(Entry<String, String> t) {
							return String.format("%s: %s", t.getKey(),
									t.getValue());
						}
					}));

			if (isFile) {
				environment.put(XPMObject.ENV_SCRIPTPATH, content);
			}

			ScriptableObject.defineProperty(scope, "env", new JSGetEnv(
					environment), 0);
			jsXPM = new XPMObject(jsContext, environment, scope, repository,
					scheduler);
			XPMObject.getLog().clear();

			final Object result;
			if (isFile)
				result = jsContext.evaluateReader(scope,
						new FileReader(content), content, 1, null);
			else
				result = jsContext.evaluateString(scope, content, "stdin", 1,
						null);

			if (result != null)
				LOGGER.info(result.toString());
			else
				LOGGER.info("Null result");

			// Object object = scope.get("Task", null);
			// if (object instanceof NativeFunction) {
			// org.mozilla.javascript.Context cx2 =
			// org.mozilla.javascript.Context
			// .enter();
			// ((NativeFunction) object).call(cx2, scope, scope, null);
			// org.mozilla.javascript.Context.exit();
			// }

		} catch (WrappedException e) {
			LOGGER.printException(Level.INFO, e.getCause());
			error = 2;
			errorMsg = e.getCause().toString() + "\n[in] " + e.toString();
			errorMsg += "\n" + e.getScriptStackTrace();
		} catch (JavaScriptException e) {
			LOGGER.printException(Level.INFO, e);
			error = 3;
			errorMsg = e.toString();
			errorMsg += "\n" + e.getScriptStackTrace();
		} catch (Exception e) {
			LOGGER.printException(Level.INFO, e);
			error = 1;
			errorMsg = e.toString();
		} finally {
			// Exit context
			org.mozilla.javascript.Context.exit();
		}

		ArrayList<Object> list = new ArrayList<Object>();
		list.add(error);
		list.add(errorMsg);
		if (jsXPM != null) {
			list.add(XPMObject.getLog());
		}
		XPMObject.resetLog();
		return list;
	}

	/**
	 * Add a command line job
	 * 
	 * @throws DatabaseException
	 */
	public boolean runCommand(String name, int priority, Object[] command,
			Object[] envArray, String workingDirectory, Object[] depends,
			Object[] readLocks, Object[] writeLocks) throws DatabaseException {
		Map<String, String> env = arrayToMap(envArray);
		LOGGER.info(
				"Running command %s [%s] (priority %d); read=%s, write=%s; environment={%s}",
				name, Arrays.toString(command), priority,
				Arrays.toString(readLocks), Arrays.toString(writeLocks),
				Output.toString(", ", env.entrySet()));

		String[] commandArgs = new String[command.length];
		for (int i = command.length; --i >= 0;)
			commandArgs[i] = command[i].toString();

		CommandLineTask job = new CommandLineTask(scheduler, name, commandArgs,
				env, new File(workingDirectory));

		// Process locks
		for (Object depend : depends) {
			Resource resource = scheduler.getResource((String) depend);
			if (resource == null)
				throw new RuntimeException("Resource " + depend
						+ " was not found");
			job.addDependency(resource, LockType.GENERATED);
		}

		// We have to wait for read lock resources to be generated
		for (Object readLock : readLocks) {
			Resource resource = scheduler.getResource((String) readLock);
			if (resource == null)
				throw new RuntimeException("Resource " + readLock
						+ " was not found");
			job.addDependency(resource, LockType.READ_ACCESS);
		}

		// Write locks
		for (Object writeLock : writeLocks) {
			final String id = (String) writeLock;
			Resource resource = scheduler.getResource(id);
			if (resource == null) {
				resource = new SimpleData(scheduler, id,
						LockMode.EXCLUSIVE_WRITER, false);
				resource.register(job);
			}
			job.addDependency(resource, LockType.WRITE_ACCESS);
		}

		scheduler.add(job);
		return true;
	}

	/**
	 * Utility function that transforms an array with paired values into a map
	 * 
	 * @param envArray
	 *            The array, must contain an even number of elements
	 * @return a map
	 */
	private Map<String, String> arrayToMap(Object[] envArray) {
		Map<String, String> env = new TreeMap<String, String>();
		for (Object x : envArray) {
			Object[] o = (Object[]) x;
			if (o.length != 2)
				// FIXME: should be a proper one
				throw new RuntimeException();
			env.put((String) o[0], (String) o[1]);
		}
		return env;
	}
}