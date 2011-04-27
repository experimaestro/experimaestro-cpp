package sf.net.experimaestro.utils;

import static java.lang.String.format;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.UniqueTag;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.xml.XMLObject;
import org.mozilla.javascript.xmlimpl.XMLLibImpl;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.utils.log.Logger;

public class JSUtils {
	final static private Logger LOGGER = Logger.getLogger();

	/**
	 * Get an object from a scriptable
	 * 
	 * @param object
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T get(Scriptable scope, String name, NativeObject object) {
		final Object _value = object.get(name, scope);
		if (_value == UniqueTag.NOT_FOUND)
			throw new RuntimeException(format("Could not find property '%s'",
					name));
		return (T) _value;
	}

	/**
	 * Get an object from a scriptable
	 * 
	 * @param object
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T get(Scriptable scope, String name, NativeObject object,
			T defaultValue) {
		final Object _value = object.get(name, scope);
		if (_value == UniqueTag.NOT_FOUND)
			return defaultValue;
		return (T) _value;
	}

	/**
	 * Transforms a DOM node to a E4X scriptable object
	 * 
	 * @param node
	 * @param cx
	 * @param scope
	 * @return
	 */
	public static Object domToE4X(Node node, Context cx, Scriptable scope) {
		if (node == null) {
			LOGGER.info("XML is null");
			return Context.getUndefinedValue();
		}
		if (node instanceof Document)
			node = ((Document) node).getDocumentElement();
		
		
		LOGGER.info("XML is of type %s [%s]; %s", node.getClass(),
				XMLUtils.toString(node),
				node.getUserData("org.mozilla.javascript.xmlimpl.XmlNode"));
		return cx.newObject(scope, "XML", new Node[] { node });
	}

	/**
	 * Transform objects into an XML node
	 * 
	 * @param object
	 * @return
	 */
	public static Node toDOM(Object object) {
		// Unwrap if needed
		if (object instanceof Wrapper)
			object = ((Wrapper) object).unwrap();

		// It is already a DOM node
		if (object instanceof Node)
			return (Node) object;

		if (object instanceof XMLObject) {
			XMLObject xmlObject = (XMLObject) object;
			String className = xmlObject.getClassName();

			// Use Rhino implementation for XML objects
			if (className.equals("XML")) {
				// FIXME: this strips all whitespaces!
				Node node = XMLLibImpl.toDomNode(object);
 				LOGGER.debug("Cloned node [%s / %s] from [%s]", node.getClass(), XMLUtils.toStringObject(node), object.toString());
				return node;
			}

			// Should be an XMLList
			if (className.equals("XMLList")) {
				LOGGER.debug("Transforming from XMLList [%s]", object);
				IdScriptableObject xmlList = (IdScriptableObject) xmlObject;
				DocumentBuilder docBuilder;
				try {
					docBuilder = Task.dbFactory.newDocumentBuilder();
				} catch (ParserConfigurationException e) {
					throw new RuntimeException(e);
				}
				Document doc = docBuilder.newDocument();
				DocumentFragment fragment = doc.createDocumentFragment();

				for (Object _id : xmlList.getIds()) {
					Node dom = toDOM(xmlList.get((Integer) _id, xmlList));
					doc.adoptNode(dom);
					fragment.appendChild(dom);
				}

				return fragment;
			}

			throw new RuntimeException(format(
					"Not implemented: convert %s to XML", className));

		}

		throw new RuntimeException("Class %s cannot be converted to XML");
	}

	/**
	 * Returns true if the object is XML
	 * 
	 * @param input
	 * @return
	 */
	public static boolean isXML(Object input) {
		return input instanceof XMLObject;
	}
}
