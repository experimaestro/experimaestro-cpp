package sf.net.experimaestro.utils;

import static java.lang.String.format;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.UniqueTag;
import org.mozilla.javascript.xml.XMLObject;
import org.mozilla.javascript.xmlimpl.XMLLibImpl;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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
	public static Scriptable domToE4X(Node node, Context cx, Scriptable scope) {
		return cx.newObject(scope, "XML", new Node[] { node });
	}

	/**
	 * Transform objects into an XML node
	 * 
	 * @param object
	 * @return
	 */
	public static Node toDOM(Object object) {
		// It is already a DOM node
		if (object instanceof Node)
			return (Node) object;

		if (object instanceof XMLObject) {
			XMLObject xmlObject = (XMLObject) object;
			String className = xmlObject.getClassName();

			// Use Rhino implementation for XML objects
			if (className.equals("XML")) {
				// FIXME: this stripts all whitespaces!
				return XMLLibImpl.toDomNode(object);
//				DocumentBuilderFactory builderFactory = DocumentBuilderFactory
//						.newInstance();
//				Document document;
//				try {
//					DocumentBuilder builder = builderFactory
//							.newDocumentBuilder();
//					document = builder.parse(new InputSource(new StringReader(
//							"<root>" + object.toString() + "</root>")));
//				} catch (SAXException e) {
//					LOGGER.error(object.toString());
//					throw new RuntimeException(e);
//				} catch (IOException e) {
//					throw new RuntimeException(e);
//				} catch (ParserConfigurationException e) {
//					throw new RuntimeException(e);
//				}
//				Element el = document.getDocumentElement();
//				NodeList nodes = el.getChildNodes();
//				if (nodes.getLength() == 1)
//					return nodes.item(0);
//				DocumentFragment fragment = document.createDocumentFragment();
//				for (int i = 0; i < nodes.getLength(); i++)
//					fragment.appendChild(nodes.item(i));
//				return fragment;

			}

			// Should be an XMLList
			if (className.equals("XMLList")) {
				LOGGER.info("Transforming from XMLList [%s]", object);
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
