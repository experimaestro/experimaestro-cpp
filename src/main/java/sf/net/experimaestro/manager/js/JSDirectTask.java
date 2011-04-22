package sf.net.experimaestro.manager.js;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

import sf.net.experimaestro.exceptions.ExperimaestroException;
import sf.net.experimaestro.utils.log.Logger;

public class JSDirectTask extends JSAbstractTask {
	final static private Logger LOGGER = Logger.getLogger();

	private final Function getFunction;
	private final NativeObject jsObject;

	public JSDirectTask(JSTaskFactory jsTaskFactory, Scriptable jsScope,
			NativeObject jsObject, Function getFunction) {
		super(jsTaskFactory, jsScope);
		this.jsObject = jsObject;
		this.getFunction = getFunction;

	}

	@Override
	public Scriptable jsrun() {
		LOGGER.info("[Running] task: %s", factory.getId());

		final Context cx = Context.getCurrentContext();

		// Get the inputs
		Scriptable jsInputs = getJSInputs();

		final Object returned = getFunction.call(cx, jsScope, jsObject,
				new Object[] { jsInputs });
		LOGGER.info("Returned %s", returned);
		if (returned == Undefined.instance)
			throw new ExperimaestroException(
					"Undefined returned by the function run");

		Scriptable result = (Scriptable) returned;
		LOGGER.info("[/Running] task: %s", factory.getId());

		return result;
	}

}
