package sf.net.experimaestro.manager;

import java.io.File;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;
import javax.xml.xpath.XPathFunctionResolver;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * Adds some handy functions.
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * 
 */
public class XPMXPathFunctionResolver implements XPathFunctionResolver {

	private XPathFunctionResolver resolver;

	public XPMXPathFunctionResolver(XPathFunctionResolver resolver) {
		this.resolver = resolver;
	}

	@Override
	public XPathFunction resolveFunction(QName functionName, int arity) {
		if (Manager.EXPERIMAESTRO_NS.equals(functionName.getNamespaceURI())) {
			final String name = functionName.getLocalPart();
			if ("parentPath".equals(name) && arity == 1)
				return ParentPath_1;
		}

		return resolver == null ? null : resolver.resolveFunction(functionName,
				arity);
	}

	private static final XPathFunction ParentPath_1 = new XPathFunction() {
		@Override
		public Object evaluate(List args) throws XPathFunctionException {
			return new File(argToString(args.get(0))).getParentFile();
		}
	};

	static private String argToString(Object arg) throws XPathFunctionException {
		if (arg instanceof String)
			return (String) arg;

		if (arg instanceof Boolean)
			return arg.toString();

		if (arg instanceof Double)
			return arg.toString();

		if (arg instanceof NodeList) {
			NodeList list = (NodeList) arg;
			Node node = list.item(0);
			// getTextContent is available in Java 5 and DOM 3.
			// In Java 1.4 and DOM 2, you'd need to recursively
			// accumulate the content.
			return node.getTextContent();
		}

		throw new XPathFunctionException("Could not convert argument type");
	}
}
