package sf.net.experimaestro.manager;

import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.log.Logger;


public class JSTaskInformation extends TaskInformation {
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

	private Object input;

	private Object output;

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
	public JSTaskInformation(Context context, Scriptable scope,
			NativeObject jsObject) {
		super((String) JSUtils.get(scope, "id", jsObject), (String) JSUtils
				.get(scope, "version", jsObject), null);
		this.context = context;
		this.scope = scope;
		this.jsObject = jsObject;

		input = JSUtils.get(scope, "parameters", jsObject);
		output = JSUtils.get(scope, "outputs", jsObject);

	}

	@Override
	String getDocumentation() {
		return JSUtils.get(scope, "description", jsObject).toString();
	}

	@Override
	public Task create() {
		Object fObj = JSUtils.get(scope, "create", jsObject);

		if (!(fObj instanceof Function))
			throw new RuntimeException("create is undefined or not a function.");

		Function f = (Function) fObj;
		Object result = f.call(context, scope, scope, new Object[] {});
		LOGGER.info("Created a new experiment: %s (%s)", result,
				result.getClass());
		return new JSTask(context, scope, (NativeObject) result);
	}

	@Override
	public Map<QName, NamedParameter> getParameters() {
		NativeObject inputs = (NativeObject) JSUtils.get(scope, "input",
				jsObject);

		return null;
	}
}
