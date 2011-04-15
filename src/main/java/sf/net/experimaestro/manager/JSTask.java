package sf.net.experimaestro.manager;

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
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import sf.net.experimaestro.utils.log.Logger;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.XMLUtils;

/**
 * Task as implemented by a javascript object
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class JSTask extends Task {
	final static private Logger LOGGER = Logger.getLogger();

	private final Context jsContext;
	private final Scriptable jsScope;
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

		// Get (optional) set parameter function
		setParameterFunction = (Function) JSUtils.get(jsScope, "getParameters",
				jsObject, null);
	}

	@Override
	public void setParameter(DotName id, Node value) {
		// FIXME
		LOGGER.debug("Setting parameter %s", id);
		final String name = id.getName();

		if (setParameterFunction == null) {
			// FIXME: should do some checking with getParameters()
			short nodeType = value.getNodeType();
			if (nodeType == Node.ELEMENT_NODE)
				jsObject.put(name, jsObject,
						JSUtils.domToE4X(value, jsContext, jsScope));
			else if (nodeType == Node.DOCUMENT_FRAGMENT_NODE) {
				// Embed in a list
				NodeList childNodes = value.getChildNodes();
				Scriptable[] nodes = new Scriptable[childNodes.getLength()];
				for (int i = 0; i < nodes.length; i++)
					nodes[i] = JSUtils.domToE4X(childNodes.item(i), jsContext,
							jsScope);
				Scriptable object = jsContext.newObject(jsScope, "XMLList",
						nodes);
				LOGGER.debug(value.getTextContent());
				jsObject.put(name, jsObject, object);

			} else
				throw new RuntimeException("Cannot handle type " + nodeType);
		} else {
			setParameterFunction.call(jsContext, jsScope, jsScope,
					new Object[] { name, value });
		}
	}

	@Override
	public Map<DotName, NamedParameter> getParameters() {
		return taskFactory.getInputs();
	}

	@Override
	public Map<DotName, QName> getOutputs() {
		return null;
	}

	@Override
	public Document run() {
		LOGGER.info("[Running] task: %s", taskFactory.id);
		Scriptable result = (Scriptable) runFunction.call(jsContext, jsScope,
				jsObject, new Object[] {});
		LOGGER.info("[/Running] task: %s", taskFactory.id);

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
