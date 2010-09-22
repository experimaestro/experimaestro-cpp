package bpiwowar.expmanager.experiments;

import static java.lang.String.format;

import java.util.Map;
import java.util.TreeMap;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

import bpiwowar.log.Logger;

public class JSExperiment extends Experiment {
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

	public JSExperiment(Context context, Scriptable scope, NativeObject result) {
		this.context = context;
		this.scope = scope;
		this.jsObject = result;

		runFunction = (Function) JSHandler.get(scope, "run", jsObject);

		setParameterFunction = (Function) JSHandler.get(scope, "getParameters",
				jsObject);
		getParametersFunction = (Function) JSHandler.get(scope,
				"setParameters", jsObject);
		getOutputsFunction = (Function) JSHandler.get(scope, "getOutputs",
				jsObject);

		parametersProperty = (NativeObject) JSHandler.get(scope, "parameters",
				jsObject);
		outputsProperty = (NativeObject) JSHandler.get(scope, "outputs",
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
	public Map<QName, Parameter> getParameters() {
		return null;
	}

	@Override
	public Map<QName, Type> getOutputs() {
		return null;
	}

	@Override
	public Map<QName, Object> run() {
		Object result = runFunction
				.call(context, scope, scope, new Object[] {});
		LOGGER.info("Ran the experiment: %s (%s)", result, result.getClass());

		if (result instanceof NativeObject) {
			Map<QName, Object> map = new TreeMap<QName, Object>();
			NativeObject object = (NativeObject) result;
			Object[] keys = object.getAllIds();
			for (int i = 0; i < keys.length; i++) {
				Object jsValue = object.get(i, scope);
				map.put(new QName(keys[i].toString()), jsValue);
			}
			return map;
		}

		throw new RuntimeException(format(
				"Output should be an object (class is %s)", result.getClass()));

	}

}
