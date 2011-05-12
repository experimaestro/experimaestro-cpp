package sf.net.experimaestro.manager.js;

import static java.lang.String.format;

import javax.xml.xquery.XQDataSource;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

import sf.net.experimaestro.manager.Task;
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

	/**
	 * The Task object
	 */
	private NativeObject jsObject;

	/**
	 * The run Function
	 */
	private Function runFunction;

	public JSTask() {
	}

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

	public Scriptable jsrun(boolean convertToE4X) {
		LOGGER.info("[Running] task: %s", factory.getId());
		Scriptable result = (Scriptable) runFunction.call(
				Context.getCurrentContext(), jsScope, jsObject,
				new Object[] { (Scriptable) jsObject.get("inputs", jsObject) });
		LOGGER.info("[/Running] task: %s", factory.getId());
		return result;
	}

	@Override
	protected void init(Task _other) {
		JSTask other = (JSTask) _other;
		super.init(other);
		jsObject = other.jsObject;
		runFunction = other.runFunction;
	}
}
