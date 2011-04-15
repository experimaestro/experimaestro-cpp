package sf.net.experimaestro.manager;

import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;

import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.log.Logger;


public class JSTask extends Task {
	final static private Logger LOGGER = Logger.getLogger();

	private final Context context;
	private final Scriptable scope;
	private final NativeObject jsObject;

	/**
	 * The run Function
	 */
	private Function runFunction;

	/**
	 * The setParameter function
	 */
	private Function setParameterFunction;

	/**
	 * The function that returns the set of actual parameters
	 */
	private Function getParametersFunction;

	/**
	 * If the set/get parameters is not used, we rely on this for checking
	 */
	private NativeObject parametersProperty;

	/**
	 * The function that returns the set of actual outputs
	 */
	private Function getOutputsFunction;

	/**
	 * If the get
	 */
	private NativeObject outputsProperty;

	public JSTask(Context context, Scriptable scope, NativeObject result) {
		this.context = context;
		this.scope = scope;
		this.jsObject = result;

		runFunction = (Function) JSUtils.get(scope, "run", jsObject);

		setParameterFunction = (Function) JSUtils.get(scope, "getParameters",
				jsObject);
		getParametersFunction = (Function) JSUtils.get(scope,
				"setParameters", jsObject);
		getOutputsFunction = (Function) JSUtils.get(scope, "getOutputs",
				jsObject);

		parametersProperty = (NativeObject) JSUtils.get(scope, "parameters",
				jsObject);
		outputsProperty = (NativeObject) JSUtils.get(scope, "outputs",
				jsObject);
	}

	@Override
	public void setParameter(QName id, Object value) {
		final String name = id.getName();

		if (setParameterFunction == null) {
			// FIXME: should do some checking with getParameters()
			jsObject.put(name, jsObject, Context.toObject(value, jsObject));
		} else {
			setParameterFunction.call(context, scope, scope, new Object[] {
					name, value });
		}
	}

	@Override
	public Map<QName, NamedParameter> getParameters() {
		return null;
	}

	@Override
	public Map<QName, Type> getOutputs() {
		return null;
	}

	@Override
	public Document run() {
		Scriptable result = (Scriptable) runFunction.call(context, scope,
				scope, new Object[] {});
		LOGGER.info("Ran the experiment: %s (%s)", result, result.getClass());
		return (Document) JSUtils.toDOM(result);
	}

}
