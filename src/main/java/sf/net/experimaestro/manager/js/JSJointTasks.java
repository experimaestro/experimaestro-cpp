package sf.net.experimaestro.manager.js;

import static java.lang.String.format;

import java.util.Map.Entry;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.w3c.dom.Node;

import sf.net.experimaestro.manager.DotName;
import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.utils.log.Logger;

/**
 * Meta-task composed of several interconnected tasks defined in JavaScript
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class JSJointTasks extends JSTask {
	final static private Logger LOGGER = Logger.getLogger();

	/**
	 * Standard initialisation
	 * 
	 * @param taskFactory
	 * @param context
	 * @param scope
	 */
	JSJointTasks(JSJointTaskFactory taskFactory, Context context,
			Scriptable scope, NativeObject jsObject) {
		super(taskFactory, context, scope, jsObject);

		// Create a JavaScript map
		Scriptable jsMap = Context.getCurrentContext().newObject(jsScope,
				"Object", new Object[] {});
		jsObject.defineProperty("tasks", jsMap, ScriptableObject.READONLY);

		// Creates the sub-tasks and map them to javascript
		for (Entry<String, TaskFactory> entry : taskFactory.getSubtasks()
				.entrySet()) {
			String id = entry.getKey();
			TaskFactory factory = entry.getValue();
			LOGGER.debug("Creating sub-task %s", id);

			Task task = factory.create();
			tasks.put(id, task);

			jsMap.put(
					id,
					jsMap,
					Context.getCurrentContext().newObject(jsScope, "XPMTask",
							new Object[] { task }));
		}

	}

	/**
	 * Add a new task
	 * 
	 * @param qualifier
	 *            The qualifier for this task
	 * @param experiment
	 */
	public void put(String qualifier, Task experiment) {
		tasks.put(qualifier, experiment);
	}

}
