package sf.net.experimaestro.utils;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import sf.net.experimaestro.exceptions.ExperimaestroException;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.utils.log.Logger;

/**
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * 
 */
public class XMLUtils {
	final static private Logger LOGGER = Logger.getLogger();

	/**
	 * Convert an XML node into a string
	 * 
	 * @param node
	 *            The XML node to convert
	 * @return A string representing the XML element
	 */
	static public final String toString(Node node) {
		try {
			DOMImplementationRegistry registry = DOMImplementationRegistry
					.newInstance();

			DOMImplementationLS impl = (DOMImplementationLS) registry
					.getDOMImplementation("LS");

			LSSerializer writer = impl.createLSSerializer();
			return writer.writeToString(node);
		} catch (Exception e) {
			return e.toString();
		}
	}

	/**
	 * Wrapper so that the XML is transformed into a string on demand
	 * 
	 * @param node
	 * @return
	 */
	static public final Object toStringObject(final Node node) {
		return new Object() {
			private String s;

			@Override
			public String toString() {
				if (s == null)
					s = XMLUtils.toString(node);
				return s;
			}
		};
	}

	public static String toString(NodeList nodes) {
		try {
			StringWriter stw = new StringWriter();
			Transformer serializer = TransformerFactory.newInstance()
					.newTransformer();
			for (int i = 0; i < nodes.getLength(); i++) {
				Node item = nodes.item(i);
				switch (item.getNodeType()) {
				case Node.TEXT_NODE:
					stw.append(item.getTextContent());
					break;
				default:
					serializer.transform(new DOMSource(item), new StreamResult(
							stw));
				}
			}
			return stw.toString();
		} catch (Exception e) {
			return e.toString();
		}
	}

	final static Pattern qnamePattern;
	static {
		try {
			// (?:\\{\\[\\p{L}:-\\.\\d]+\\)}|(\\p{L}):)?(\\w+)
			qnamePattern = Pattern
					.compile("(?:\\{(\\w[\\w\\.:]+)\\}|(\\w+):)?([\\w-\\.]+)");
		} catch (PatternSyntaxException e) {
			LOGGER.error("Could not initialise the pattern: %s", e);
			throw e;
		}
	}

	/**
	 * Parse a QName from a string following this format:
	 * <ul>
	 * <li>localName</li>
	 * <li>{namespace}name</li>
	 * <li>prefix:name</li>
	 * </ul>
	 * 
	 * @param qname
	 * @return
	 */
	public static QName parseQName(String qname, Element context,
			Map<String, String> prefixes) {
		Matcher matcher = qnamePattern.matcher(qname);
		if (!matcher.matches())
			throw new ExperimaestroException("Type [%s] is not a valid type",
					qname);

		String url = matcher.group(1);
		String prefix = matcher.group(2);
		if (prefix != null) {
			url = context.lookupNamespaceURI(prefix);
			if (url == null && prefixes != null)
				url = prefixes.get(prefix);
			if (url == null)
				throw new ExperimaestroException(
						"Type [%s] is not a valid type: namespace prefix [%s] not bound",
						qname, prefix);
		}

		String name = matcher.group(3);
		return new QName(url, name);

	}

	/**
	 * Finds a child with a given qualified name
	 * 
	 * @param element
	 * @param qName
	 * @return
	 */
	public static Node getChild(Element element, QName qName) {
		NodeList list = element.getChildNodes();
		Element candidate = null;
		String ns = qName.getNamespaceURI();
		String name = qName.getLocalPart();
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			String nodeNS = node.getNamespaceURI();
			if (node instanceof Element && node.getLocalName().equals(name)
					&& ((ns == null && nodeNS == null) || (ns.equals(nodeNS)))) {
				if (candidate != null)
					throw new ExperimaestroException(
							"Two children with the same name [%s]", qName);
				candidate = (Element) node;
			}
		}
		return candidate;
	}

	/**
	 * Finds a child with a given qualified name
	 * 
	 * @param element
	 * @param qName
	 * @return An iterator
	 */
	public static Iterable<Element> childIterator(Element element, QName qName) {
		ArrayList<Element> x = new ArrayList<Element>();
		NodeList list = element.getChildNodes();
		String ns = qName.getNamespaceURI();
		String name = qName.getLocalPart();
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			String nodeNS = node.getNamespaceURI();
			if (node instanceof Element && node.getLocalName().equals(name)
					&& ((ns == null && nodeNS == null) || (ns.equals(nodeNS)))) {
				x.add((Element) node);
			}
		}
		return x;
	}

	private final static DocumentBuilderFactory dbFactory = DocumentBuilderFactory
			.newInstance();
	private static DocumentBuilder documentBuilder;
	static {
		try {
			dbFactory.setNamespaceAware(true);
		    documentBuilder = dbFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new ExperimaestroException(
					"Could not build a document builder", e);
		}
	}

	/**
	 * Creates a new XML document
	 * 
	 * @return
	 */
	public static Document newDocument() {
		return documentBuilder.newDocument();
	}


}
