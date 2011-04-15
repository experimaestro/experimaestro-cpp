package sf.net.experimaestro.manager.js;

import static java.lang.String.format;

import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import sf.net.experimaestro.manager.DotName;
import sf.net.experimaestro.manager.NamedParameter;
import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.log.Logger;

/**
 * Task as implemented by a javascript object
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class JSTask extends Task {
	final static private Logger LOGGER = Logger.getLogger();

	protected final Context jsContext;
	final Scriptable jsScope;
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
		super(taskFactory);

		this.jsContext = jsContext;
		this.jsScope = jsScope;
		this.jsObject = jsObject;

		// Get the run function
		runFunction = (Function) JSUtils.get(jsScope, "run", jsObject, null);
		if (runFunction == null) {
			throw new RuntimeException(
					format("Could not find the function run() in the object"));
		}

		// Set inputs
		Scriptable jsInputs = Context.getCurrentContext().newObject(jsScope, "Object",
				new Object[] {});
		jsObject.put("inputs", jsObject, jsInputs);

	}

	@Override
	public void setParameter(DotName id, Node value) {
		// FIXME: should cope with dot names
		LOGGER.debug("[set] parameter %s to %s", id, value);
		final String name = id.getName();

		// FIXME: should do some checking with getParameters()

		short nodeType = value.getNodeType();
		Scriptable jsInput = null;

		if (nodeType == Node.ELEMENT_NODE) {
			jsInput = JSUtils.domToE4X(value, jsContext, jsScope);
		} else if (nodeType == Node.DOCUMENT_FRAGMENT_NODE) {
			// Embed in a list
			NodeList childNodes = value.getChildNodes();
			Scriptable[] nodes = new Scriptable[childNodes.getLength()];
			for (int i = 0; i < nodes.length; i++)
				nodes[i] = JSUtils.domToE4X(childNodes.item(i), jsContext,
						jsScope);
			jsInput = jsContext.newObject(jsScope, "XMLList", nodes);
		}

		if (jsInput == null)
			throw new RuntimeException("Cannot handle type " + nodeType);

		// Set this value

		Scriptable jsInputs = (Scriptable) jsObject.get("inputs", jsObject);
		jsInputs.put(name, jsInputs, jsInput);

		LOGGER.debug("[/set] parameter %s (task %s)", name, factory.getId());
	}

	@Override
	public Map<DotName, NamedParameter> getParameters() {
		return factory.getInputs();
	}

	@Override
	public Map<DotName, QName> getOutputs() {
		return null;
	}

	public Scriptable jsrun() {
		LOGGER.info("[Running] task: %s", factory.getId());
		Scriptable result = (Scriptable) runFunction.call(jsContext, jsScope,
				jsObject, new Object[] {});
		LOGGER.info("[/Running] task: %s", factory.getId());
		return result;
	}

	@Override
	public Document run() {
		Scriptable result = jsrun();
		Node node = JSUtils.toDOM(result);
		if (node instanceof Document)
			return (Document) node;

		// Just a node - convert do document

		// first of all we request out
		// DOM-implementation:
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		// then we have to create document-loader:
		DocumentBuilder loader;
		try {
			loader = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException();
		}

		// creating a new DOM-document...
		Document document = loader.newDocument();
		node = node.cloneNode(true);
		document.adoptNode(node);
		document.appendChild(node);

		return document;
	}

}
