package sf.net.experimaestro.manager.js;

import static java.lang.String.format;


import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Element;

import sf.net.experimaestro.manager.DotName;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.log.Logger;

/**
 * Task as implemented by a javascript object
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class JSTask extends JSAbstractTask {
	final static private Logger LOGGER = Logger.getLogger();

	private final NativeObject jsObject;

	/**
	 * The run Function
	 */
	private Function runFunction;

	/**
	 * Initialise a new task from a JavaScript object
	 * 
	 * @param taskFactory
	 *            The task factory
	 * @param jsContext
	 *            The context for evaluation JavaScript code
	 * @param jsScope
	 *            The scope for evaluating JavaScript code
	 * @param jsObject
	 *            The JavaScript object
	 */
	public JSTask(TaskFactory taskFactory, Context jsContext,
			Scriptable jsScope, NativeObject jsObject) {
		super(taskFactory, jsScope);

		this.jsObject = jsObject;

		// Get the run function
		runFunction = (Function) JSUtils.get(jsScope, "run", jsObject, null);
		if (runFunction == null) {
			throw new RuntimeException(
					format("Could not find the function run() in the object"));
		}

		// Set inputs
		Scriptable jsInputs = Context.getCurrentContext().newObject(jsScope,
				"Object", new Object[] {});
		jsObject.put("inputs", jsObject, jsInputs);

	}

	@Override
	public boolean setParameter(DotName id, Element element, boolean direct) {
		if (super.setParameter(id, element, direct))
			return true;

		// If we have more than one level we don't know how to handle this
		if (id.size() != 1)
			return false;

		// Let's go
		LOGGER.debug("[set] parameter %s to %s", id, element);

		final String name = id.getName();

		Scriptable jsInput = toE4X(element);

		Scriptable jsInputs = (Scriptable) jsObject.get("inputs", jsObject);
		jsInputs.put(name, jsInputs, jsInput);

		LOGGER.debug("[/set] parameter %s (task %s)", name, factory.getId());
		return true;
	}

	public Scriptable jsrun() {
		LOGGER.info("[Running] task: %s", factory.getId());
		Scriptable result = (Scriptable) runFunction.call(Context.getCurrentContext(), jsScope,
				jsObject, new Object[] { (Scriptable) jsObject.get("inputs", jsObject) });
		LOGGER.info("[/Running] task: %s", factory.getId());
		return result;
	}

}
