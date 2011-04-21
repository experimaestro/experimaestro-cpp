package sf.net.experimaestro.manager.js;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.w3c.dom.Element;

import sf.net.experimaestro.exceptions.ExperimaestroException;
import sf.net.experimaestro.manager.DotName;
import sf.net.experimaestro.utils.log.Logger;

public class JSConfigurationTask extends JSAbstractTask {
	final static private Logger LOGGER = Logger.getLogger();

	private final Function getFunction;
	private final NativeObject jsObject;

	private Scriptable jsInputs;

	public JSConfigurationTask(JSTaskFactory jsTaskFactory, Scriptable jsScope,
			NativeObject jsObject, Function getFunction) {
		super(jsTaskFactory, jsScope);
		this.jsObject = jsObject;
		this.getFunction = getFunction;
		jsInputs = Context.getCurrentContext().newObject(jsScope, "Object",
				new Object[] {});

	}

	@Override
	public boolean setParameter(DotName id, Element value, boolean direct) {
		// Let the superclass handle this first
		if (super.setParameter(id, value, direct))
			return true;

		// If we have more than one level we don't know how to handle this
		if (id.size() != 1)
			return false;

		// Set the input value
		jsInputs.put(id.getName(), jsInputs, toE4X(value));
		return true;
	}

	@Override
	public Scriptable jsrun() {
		LOGGER.info("[Running] task: %s", factory.getId());
		final Object call = getFunction.call(
				Context.getCurrentContext(), jsScope, jsObject,
				new Object[] { jsInputs });
		LOGGER.info("Returned %s", call);
		if (call == Undefined.instance)
			throw new ExperimaestroException("Undefined returned by the function get()");
		
		Scriptable result = (Scriptable) call;
		LOGGER.info("[/Running] task: %s", factory.getId());

		return result;
	}

}
