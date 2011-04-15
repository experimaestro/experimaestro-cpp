package sf.net.experimaestro.manager.js;

import java.util.Map;
import java.util.TreeMap;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.w3c.dom.Node;

import sf.net.experimaestro.manager.DotName;
import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.utils.Converter;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.Maps;
import sf.net.experimaestro.utils.log.Logger;

/**
 * Task factory as seen by JavaScript
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class TaskJSWrapper extends ScriptableObject {
	private static final long serialVersionUID = 1L;
	final static private Logger LOGGER = Logger.getLogger();

	private Task task;

	public TaskJSWrapper() {
	}

	public void jsConstructor(Scriptable task) {
		if (task != null)
			this.task = (Task) ((NativeJavaObject) task).unwrap();
	}

	@Override
	public String getClassName() {
		return "XPMTask";
	}

	// ---- JavaScript functions ----

	public Scriptable jsFunction_run() {
		if (task instanceof JSTask) {
			LOGGER.info("Running JS task %s", task.getFactory().getId());
			return ((JSTask) task).jsrun();
		}
		return (Scriptable) task.run();
	}

	/**
	 * Set a parameter
	 * 
	 * @param id
	 * @param value
	 */
	public void jsFunction_setParameter(String _id, Scriptable value) {
		DotName id = DotName.parse(_id);
		LOGGER.info("Setting input [%s] to [%s]", _id, value);

		if (value == Scriptable.NOT_FOUND)
			task.setParameter(id, (Node) null);
		else if (value instanceof Node)
			task.setParameter(id, (Node) value);
		else if (JSUtils.isXML(value)) {
			task.setParameter(id, JSUtils.toDOM(value));
		} else {
			task.setParameter(id, (String) value.toString());
		}
	}
}