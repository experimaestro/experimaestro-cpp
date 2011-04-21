package sf.net.experimaestro.manager.js;

import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

import sf.net.experimaestro.manager.DotName;
import sf.net.experimaestro.manager.Input;
import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.log.Logger;
import sun.org.mozilla.javascript.internal.UniqueTag;

/**
 * A joint task factory as defined by a JavaScript object.
 * 
 * 
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class JSJointTaskFactory extends JSTaskFactory {
	final static private Logger LOGGER = Logger.getLogger();


	/**
	 * Creates a new task information from a javascript object
	 * 
	 * @param context
	 *            The context
	 * @param scope
	 *            The scope
	 * @param jsObject
	 *            The object
	 */
	public JSJointTaskFactory(Repository repository, Scriptable scope,
			NativeObject jsObject) {
		super(scope, jsObject, repository);

		// Get the list of sub-tasks
		NativeObject jsTasks = JSUtils.get(scope, "tasks", jsObject);
		for (Object _id : jsTasks.getAllIds()) {
			String id = (String) _id;
			NativeJavaObject taskId = (NativeJavaObject) JSUtils.get(scope, id,
					jsTasks);
			TaskFactory subTask = repository.getFactory((QName) taskId.unwrap());
			getSubtasks().put(id, subTask);
		}

		// --- Now, connects

		NativeArray jsConnections = JSUtils.get(scope, "connections", jsObject);

		// Get the namespaces
		NativeObject jsNamespaces = (NativeObject) jsConnections.get(0,
				jsObject);
		for (Object _id : jsNamespaces.getAllIds()) {
			String id = (String) _id;
			String url = (String) JSUtils.get(scope, id, jsNamespaces);
			LOGGER.info("Will map %s to %s", id, url);
		}

		// Now the connections
		for (int i = 1; i < jsConnections.getLength(); i++) {
			NativeArray jsConnection = (NativeArray) jsConnections.get(i,
					jsObject);
			String from = (String) jsConnection.get(0, jsConnections);
			String fromPath = (String) jsConnection.get(1, jsConnections);
			String to = (String) jsConnection.get(2, jsConnections);
			String toSlot = (String) jsConnection.get(3, jsConnections);
			LOGGER.info("Task %s : connects %s [%s] to %s [%s]", this.getId(),
					from, fromPath, to, toSlot);
		}

	}

	@Override
	public String getDocumentation() {
		return JSUtils.get(jsScope, "description", jsObject).toString();
	}

	@Override
	public Task create() {
		// Get the "create" method
		Object fObj = JSUtils.get(jsScope, "create", jsObject);

		if (!(fObj instanceof Function))
			throw new RuntimeException("Could not find a create function");

		// Call it
		Function f = (Function) fObj;
		Context jsContext = Context.getCurrentContext();
		Object result = f.call(jsContext, jsScope, jsScope, new Object[] {});

		if (result == UniqueTag.NOT_FOUND)
			throw new RuntimeException(
					"The create function did not return an object");
		LOGGER.info("Created a new experiment: %s (%s)", result,
				result.getClass());

		JSJointTasks jsJointTasks = new JSJointTasks(this, jsContext, jsScope, (NativeObject) result);
		jsJointTasks.init();
		return jsJointTasks;
	}

	@Override
	public Map<DotName, Input> getInputs() {
		// Combine the inputs
		Map<DotName, Input> map = new TreeMap<DotName, Input>();
		for (Entry<String, TaskFactory> outer : subtasks.entrySet()) {
			for (Entry<DotName, Input> inner : outer.getValue().getInputs()
					.entrySet()) {
				map.put(new DotName(outer.getKey(), inner.getKey()),
						inner.getValue());
			}
		}

		return map;
	}

	@Override
	public Map<String, QName> getOutputs() {
		Map<String, QName> map = new TreeMap<String, QName>();
		return map;
	}
}
