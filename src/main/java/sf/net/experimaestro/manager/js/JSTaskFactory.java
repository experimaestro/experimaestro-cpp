package sf.net.experimaestro.manager.js;

import static java.lang.String.format;

import java.util.Map;
import java.util.TreeMap;

import javax.xml.namespace.QName;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import sf.net.experimaestro.manager.DotName;
import sf.net.experimaestro.manager.NamedParameter;
import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.log.Logger;

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
	Scriptable jsScope;

	/**
	 * The server
	 */
	protected NativeObject jsObject;

	protected final Context jsContext;

	protected Map<DotName, NamedParameter> inputs;

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
		this.jsContext = context;
		this.jsScope = scope;
		this.jsObject = jsObject;

		output = JSUtils.get(scope, "output", jsObject);

		// --- Get the task inputs
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
			NamedNodeMap attributes = item.getAttributes();

			Element el = (Element) item;
			Node idNode = attributes.getNamedItem("id");
			if (idNode == null)
				throw new RuntimeException(format("Input without id in %s",
						this.id));
			String idAtt = idNode.getTextContent();
			LOGGER.info("New attribute %s for task %s", idAtt, this.id);

			String type = el.getAttribute("type");

			String optional = el.getAttribute("optional");
			boolean isOptional = optional != null && optional.equals("true") ? true
					: false;

			String documentation;
			if (el.hasAttribute("help"))
				documentation = el.getAttribute("help");
			else {
				documentation = sf.net.experimaestro.utils.XMLUtils
						.toString(el.getChildNodes());
			}

			inputs.put(new DotName(idAtt), new NamedParameter(type, isOptional,
					documentation));
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
	public String getDocumentation() {
		return JSUtils.get(jsScope, "description", jsObject).toString();
	}

	@Override
	public Task create() {
		// Get the "create" method
		Object fObj = JSUtils.get(jsScope, "create", jsObject);

		if (!(fObj instanceof Function))
			throw new RuntimeException("create is undefined or not a function.");

		// Call it
		Function f = (Function) fObj;
		Object result = f.call(jsContext, jsScope, jsScope, new Object[] {});
		LOGGER.info("Created a new experiment: %s (%s)", result,
				result.getClass());
		return new JSTask(this, jsContext, jsScope, (NativeObject) result);
	}
	

	@Override
	public Map<DotName, NamedParameter> getInputs() {
		return inputs;
	}


}
