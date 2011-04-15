package sf.net.experimaestro.manager;

import static java.lang.String.format;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.namespace.QName;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import sf.net.experimaestro.log.Logger;
import sf.net.experimaestro.utils.JSUtils;


/**
 * A task factory as defined by a JavaScript object
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class JSTaskFactory extends TaskFactory {
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

	private Map<DotName, NamedParameter> inputs;

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
	public JSTaskFactory(Context context, Scriptable scope,
			NativeObject jsObject) {
		super(getQName(scope, jsObject), getPropertyString(scope, "version",
				jsObject), null);
		this.context = context;
		this.scope = scope;
		this.jsObject = jsObject;

		output = JSUtils.get(scope, "output", jsObject);

		// Get the task inputs
		Object input = JSUtils.get(scope, "input", jsObject);
		inputs = new TreeMap<DotName, NamedParameter>();
		if (!JSUtils.isXML(input))
			throw new RuntimeException(format(
					"Property input is not in XML format in %s (%s)", this.id,
					input.getClass()));
		LOGGER.info("Class is %s in %s", input.getClass(), this.id);
		Node dom = JSUtils.toDOM(input);
		NodeList list = dom.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node item = list.item(i);
			if (item.getNodeType() != Node.ELEMENT_NODE)
				continue;
			Node idNode = item.getAttributes().getNamedItem("id");
			if (idNode == null)
				throw new RuntimeException(format("Input without id in %s", this.id));
			String idAtt = idNode.getTextContent();
			LOGGER.info("New attribute %s for task %s", idAtt, this.id);
			inputs.put(new DotName(idAtt), new NamedParameter());
		}

	}

	private static QName getQName(Scriptable scope, NativeObject jsObject) {
		NativeJavaObject object = (NativeJavaObject) JSUtils.get(scope, "id",
				jsObject);
		return (QName) object.unwrap();
	}

	private static String getPropertyString(Scriptable scope, String name,
			NativeObject jsObject) {
		Object object = JSUtils.get(scope, name, jsObject);
		if (object instanceof String)
			return (String) object;

		throw new RuntimeException(format("Field %s is not a String", name));
	}

	@Override
	String getDocumentation() {
		return JSUtils.get(scope, "description", jsObject).toString();
	}

	@Override
	public Task create() {
		// Get the "create" method
		Object fObj = JSUtils.get(scope, "create", jsObject);

		if (!(fObj instanceof Function))
			throw new RuntimeException("create is undefined or not a function.");

		// Call it
		Function f = (Function) fObj;
		Object result = f.call(context, scope, scope, new Object[] {});
		LOGGER.info("Created a new experiment: %s (%s)", result,
				result.getClass());
		return new JSTask(this, context, scope, (NativeObject) result);
	}

	@Override
	public Map<DotName, NamedParameter> getInputs() {
		return inputs;
	}
}
