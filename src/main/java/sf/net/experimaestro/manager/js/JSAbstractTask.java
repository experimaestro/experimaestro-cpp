package sf.net.experimaestro.manager.js;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.utils.JSUtils;

public abstract class JSAbstractTask extends Task {
	final protected Scriptable jsScope;

	public JSAbstractTask(TaskFactory information, Scriptable jsScope) {
		super(information);
		this.jsScope = jsScope;
	}

	protected Scriptable toE4X(Element value) {
		int nodeType = value.getNodeType();
		Scriptable jsInput = null;
		Context jsContext = Context.getCurrentContext();
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
		return jsInput;
	}

	protected Document getDocument(Scriptable result) {
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

	@Override
	public Document doRun() {
		return getDocument(jsrun());
	}

	abstract protected Scriptable jsrun();

}