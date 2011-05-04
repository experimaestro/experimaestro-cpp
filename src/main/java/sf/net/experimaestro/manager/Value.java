package sf.net.experimaestro.manager;

import static java.lang.String.format;

import java.lang.reflect.Constructor;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFunctionResolver;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import sf.net.experimaestro.exceptions.ExperimaestroException;
import sf.net.experimaestro.manager.Input.Connection;
import sf.net.experimaestro.utils.XMLUtils;
import sf.net.experimaestro.utils.log.Logger;

/**
 * Represents a value that can be set
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public abstract class Value {
	final static private Logger LOGGER = Logger.getLogger();

	/**
	 * The corresponding input
	 */
	Input input;

	/**
	 * Used to copy the value
	 */
	protected Value() {
	}

	/**
	 * Construct a new value
	 * 
	 * @param input
	 */
	public Value(Input input) {
		this.input = input;
	}

	/**
	 * Set the value
	 * 
	 * @param dotName
	 *            The name
	 * @param value
	 *            The value
	 */
	public abstract void set(DotName id, Document value);

	/**
	 * Process the value before it can be accessed by a task to run
	 */
	public abstract void process();

	/**
	 * Get the value
	 * 
	 * This method is called by a {@link Task} after {@link #process()}.
	 * 
	 * @return A valid XML document or null if not set
	 */
	public abstract Document get();

	/**
	 * 
	 * This method is called once by a {@link Task} after {@link #process()}.
	 * 
	 * @param task
	 */
	void processConnections(Task task) {
		// Do not process if we do not have connections...
		if (input.connections.isEmpty())
			return;

		// ... or if the output is null
		Document document = get();
		if (document == null) {
			LOGGER.warn("Cannot set the value of connections since we have a null value");
			return;
		}

		LOGGER.debug("Before processing connections, document is [%s]",
				XMLUtils.toStringObject(document));
		for (Connection connection : input.connections) {
			String expr = connection.path;
			String exprFrom = null;
			for (int i = connection.from.size(); --i >= 0;) {
				String step = format("xp:outputs[@xp:name='%s']",
						connection.from.get(i));
				if (exprFrom == null)
					exprFrom = step;
				else
					exprFrom = step + "/" + exprFrom;
			}
			try {
				LOGGER.debug("Processing connection [%s, %s, %s]", exprFrom, expr,
						connection.to);

				XPath xpath = XPathFactory.newInstance().newXPath();
				xpath.setNamespaceContext(connection.getContext());
				XPathFunctionResolver old = xpath.getXPathFunctionResolver();
				xpath.setXPathFunctionResolver(new XPMXPathFunctionResolver(old));

				Node item = document.getDocumentElement();
				if (exprFrom != null) {
					XPathExpression expressionFrom = xpath.compile(exprFrom);
					NodeList list = (NodeList) expressionFrom.evaluate(item,
							XPathConstants.NODESET);
					if (list.getLength() > 1)
						throw new ExperimaestroException(
								"Too much answers (%d) for XPath [%s]",
								list.getLength(), exprFrom);
					if (list.getLength() == 0) {
						LOGGER.warn("No answer for XPath [%s]", exprFrom);
						continue;
					}
					item = list.item(0);
				}

				XPathExpression expression = xpath.compile(expr);
				NodeList list = (NodeList) expression.evaluate(item,
						XPathConstants.NODESET);

				if (list.getLength() > 1)
					throw new ExperimaestroException(
							"Too much answers (%d) for XPath [%s]",
							list.getLength(), expr);
				if (list.getLength() != 1) {
					LOGGER.warn("No answer for XPath [%s]", expr);
					continue;
				}
				item = list.item(0);

				LOGGER.debug("Answer is [%s of type %d]",
						XMLUtils.toStringObject(item), item.getNodeType());
				Document newDoc = XMLUtils.newDocument();
				item = item.cloneNode(true);
				if (item instanceof Document)
					item = ((Document) item).getDocumentElement();
				newDoc.adoptNode(item);
				newDoc.appendChild(item);
				task.setParameter(connection.to, newDoc);

			} catch (XPathExpressionException e) {
				throw new ExperimaestroException(e,
						"Cannot evaluate XPath [%s]", expr);
			}

		}
	}

	final public Value copy() {
		try {
			Constructor<? extends Value> constructor = this.getClass()
					.getConstructor(new Class<?>[] {});
			Value copy = constructor.newInstance(new Object[] {});
			copy.init(this);
			return copy;
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable t) {
			throw new ExperimaestroException(t);
		}
	}

	protected void init(Value other) {
		this.input = other.input;
	}

}
