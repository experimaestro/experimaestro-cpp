package sf.net.experimaestro.manager.js;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import sf.net.experimaestro.locks.LockType;
import sf.net.experimaestro.scheduler.CommandLineTask;
import sf.net.experimaestro.scheduler.Resource;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.log.Logger;

import com.sleepycat.je.DatabaseException;

/**
 * Scheduler as seen by JavaScript
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class JSScheduler extends ScriptableObject {
	final static private Logger LOGGER = Logger.getLogger();

	private static final long serialVersionUID = 1L;

	public static final String CLASSNAME = "Scheduler";

	Scheduler scheduler;

	public JSScheduler() {
	}
	
	public void jsConstructor(Scriptable scheduler) {
		if (scheduler != null) {
			LOGGER.info(scheduler.toString());
			this.scheduler = (Scheduler) ((NativeJavaObject) scheduler)
					.unwrap();
		}
	}

	@Override
	public String getClassName() {
		return CLASSNAME;
	}

	// ---- JavaScript functions ----

	/**
	 * Run a command line experiment
	 * 
	 * @param jsargs
	 *            a native array
	 * @param a
	 *            E4X object
	 * @return
	 * @throws DatabaseException
	 */
	public void jsFunction_addCommandLineJob(String identifier, Object jsargs,
			Object jsresources) throws DatabaseException {
		// --- Process arguments: convert the javascript array into a Java array
		// of String
		LOGGER.debug("Adding command line job");
		final String[] args;
		if (jsargs instanceof NativeArray) {
			NativeArray array = ((NativeArray) jsargs);
			List<String> list = new ArrayList<String>();
			XPMObject.flattenArray(array, list);
			args = new String[list.size()];
			list.toArray(args);
		} else
			throw new RuntimeException(format(
					"Cannot handle an array of type %s", jsargs.getClass()));

		CommandLineTask task = new CommandLineTask(scheduler, identifier, args);

		// --- Resources
		NativeArray resources = ((NativeArray) jsresources);
		for (int i = (int) resources.getLength(); --i >= 0;) {
			NativeArray array = (NativeArray) resources.get(i, resources);
			assert array.getLength() == 2;
			Resource resource = scheduler.getResource(XPMObject.toString(array
					.get(0, array)));
			LockType lockType = LockType.valueOf(XPMObject.toString(array.get(
					1, array)));
			LOGGER.debug("Adding dependency on [%s] of tyep [%s]", resource,
					lockType);
			task.addDependency(resource, lockType);
		}

		// --- Add it
		scheduler.add(task);
	}

}