package sf.net.experimaestro.manager;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.sun.org.apache.xerces.internal.util.DOMInputSource;

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
	 * @return  A valid XML document or null if not set
	 */
	public abstract Document get();

	/**
	 * 
	 * This method is called once by a {@link Task} after {@link #process()}.
	 * 
	 * @param task
	 */
	void processConnections(Task task) {
		if (input.connections.isEmpty())
			return;
		
		Document document = get();
		if (document == null) {
			LOGGER.warn("Cannot set the value of connections since we have a null value");
			return;
		}

		LOGGER.info("Before processing connections, document is [%s]", XMLUtils.toStringObject(document));
		for(Connection connection: input.connections) {
		    try {
		    	LOGGER.info("Processing connection [%s, %s]", connection.path, connection.to);
		    	
		    	XPath xpath = XPathFactory.newInstance().newXPath();
		    	xpath.setNamespaceContext(connection);
		    	XPathExpression expression = xpath.compile(connection.path);
				NodeList list = (NodeList) expression.evaluate(document.getDocumentElement(), XPathConstants.NODESET);
				
				if (list.getLength() > 1)
					throw new ExperimaestroException("Too much answers (%d) for XPath [%s]", list.getLength(), connection.path);
				if (list.getLength() != 1) 
					LOGGER.warn("No answer for XPath [%s]", connection.path);
				else {
					Node item = list.item(0);					
					LOGGER.debug("Answer is [%s]",
							XMLUtils.toStringObject(item));
					Document newDoc = XMLUtils.newDocument();
					item = item.cloneNode(true);
					newDoc.adoptNode(item);
					newDoc.appendChild(item);
					task.setParameter(connection.to, newDoc);
				}
			
				
			} catch (XPathExpressionException e) {
				throw new ExperimaestroException("Cannot evaluate XPath [%s]", connection.path);
			}
			
		
		}
	}

}
