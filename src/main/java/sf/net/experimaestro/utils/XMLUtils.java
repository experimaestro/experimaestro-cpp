package sf.net.experimaestro.utils;

import java.io.StringWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import sf.net.experimaestro.utils.log.Logger;

public class XMLUtils {
	final static private Logger LOGGER = Logger.getLogger();

	static public final String toString(Node node) {
		try {
			DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();

			DOMImplementationLS impl = 
			    (DOMImplementationLS)registry.getDOMImplementation("LS");

			LSSerializer writer = impl.createLSSerializer();
			return writer.writeToString(node);
		} catch (Exception e) {
			return e.toString();
		}

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
}
