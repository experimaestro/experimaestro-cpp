package bpiwowar.expmanager.experiments;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

import bpiwowar.log.Logger;

public class JSInformation extends Information {
	final static private Logger LOGGER = Logger.getLogger();

	/**
	 * The scope
	 */
	Scriptable scope;

	/**
	 * The server
	 */
	private NativeObject jsObject;

	private final Context context;

	public JSInformation(Context context, Scriptable scope,
			NativeObject jsObject) {
		super((String) JSHandler.get(scope, "id", jsObject));
		this.context = context;
		this.scope = scope;
		this.jsObject = jsObject;
	}

	@Override
	String getDocumentation() {
		return JSHandler.get(scope, "description", jsObject).toString();
	}

	@Override
	public Experiment create() {
		Object fObj = JSHandler.get(scope, "create", jsObject);

		if (!(fObj instanceof Function))
			throw new RuntimeException("create is undefined or not a function.");

		Function f = (Function) fObj;
		Object result = f.call(context, scope, scope, new Object[] {});
		LOGGER.info("Created a new experiment: %s (%s)", result, result.getClass());
		return new JSExperiment(context, scope, (NativeObject) result);
	}

}
