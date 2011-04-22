package sf.net.experimaestro.manager.js;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import sf.net.experimaestro.manager.DotName;
import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.XMLUtils;
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

	public Object jsFunction_run() {
		return JSUtils.domToE4X(task.run(), Context.getCurrentContext(), this);
	}

	/**
	 * Set a parameter
	 * 
	 * @param id
	 * @param value
	 */
	public void jsFunction_setParameter(String _id, Scriptable value) {
		DotName id = DotName.parse(_id);
		LOGGER.info("Setting input [%s] to [%s] of type %s", _id, value,
				value.getClass());

		if (value == Scriptable.NOT_FOUND)
			task.setParameter(id, (Document) null);
		else if (value instanceof Element) {
			LOGGER.info("Value is an XML element");
			Document document = XMLUtils.newDocument();
			Node node = ((Element) value).cloneNode(true);
			document.adoptNode(node);
			document.appendChild(node);
			task.setParameter(id, document);
		} else if (JSUtils.isXML(value)) {
			LOGGER.info("Value is XML");
			Document document = XMLUtils.newDocument();
			Node node = ((Element) JSUtils.toDOM(value)).cloneNode(true);
			document.adoptNode(node);
			document.appendChild(node);
			task.setParameter(id, document);
		} else {
			LOGGER.info("Value will be converted to string [%s/%s]", value.getClassName(), value.getClass());
			task.setParameter(id, (String) value.toString());
		}
	}
}