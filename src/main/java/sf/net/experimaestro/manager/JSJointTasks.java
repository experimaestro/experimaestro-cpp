package sf.net.experimaestro.manager;

import static java.lang.String.format;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.xml.namespace.QName;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.w3c.dom.Node;

import sf.net.experimaestro.utils.log.Logger;


/**
 * Meta-task composed of several interconnected tasks
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
	JSJointTasks(JSJointTaskFactory taskFactory, Context context, Scriptable scope,
			NativeObject jsObject) {
		super(taskFactory, context, scope, jsObject);
		
		// Now, creates all the subtasks so that the run method
		// can access it
		jsObject.defineProperty("tasks", tasks, ScriptableObject.READONLY);
		
		for(Entry<String, TaskFactory> entry: taskFactory.getSubtasks().entrySet()) {
			String id = entry.getKey();
			TaskFactory factory = entry.getValue();
			LOGGER.debug("Creating sub-task %s", id);
			
			Task task = factory.create();
			tasks.put(id, task);
		}
	}

	/**
	 * Maps a local identifier to an experiment
	 */
	Map<String, Task> tasks = new TreeMap<String, Task>();
	
	/**
	 * Get a sub task
	 * @param id
	 * @return
	 */
	Task getTask(String id) {
		return tasks.get(id);
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

	@Override
	public void setParameter(DotName id, Node value) {
		LOGGER.debug("Setting parameter [%s]", id);
		
		// Find which experiments match
		if (id.size() >= 2) {
			Task task = tasks.get(id.get(0));
			if (task == null)
				throw new RuntimeException(format(
						"%s does not match any experiment", id.get(0)));
			task.setParameter(id.offset(1), value);
		} else {
			super.setParameter(id, value);
//			// Unqualified id
//			ArrayList<Entry<String, Task>> matches = new ArrayList<Entry<String, Task>>();
//
//			for (Entry<String, Task> outer : tasks.entrySet()) {
//				for (Entry<DotName, NamedParameter> inner : outer.getValue()
//						.getParameters().entrySet()) {
//					if (inner.getKey().getName().equals(id.get(0)))
//						matches.add(outer);
//				}
//			}
		}

	}

	@Override
	public Map<DotName, NamedParameter> getParameters() {
		Map<DotName, NamedParameter> map = new TreeMap<DotName, NamedParameter>();
		for (Entry<String, Task> outer : tasks.entrySet()) {
			for (Entry<DotName, NamedParameter> inner : outer.getValue()
					.getParameters().entrySet()) {
				map.put(new DotName(outer.getKey(), inner.getKey()),
						inner.getValue());
			}
		}

		return map;
	}

	@Override
	public Map<DotName, QName> getOutputs() {
		Map<DotName, QName> map = new TreeMap<DotName, QName>();

		for (Entry<String, Task> outer : tasks.entrySet()) {
			for (Entry<DotName, QName> inner : outer.getValue().getOutputs()
					.entrySet()) {
				map.put(new DotName(outer.getKey(), inner.getKey()),
						inner.getValue());
			}
		}

		return map;
	}
	
	

}
