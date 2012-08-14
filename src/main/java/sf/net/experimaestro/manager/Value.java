/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
 *
 * experimaestro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * experimaestro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 */

package sf.net.experimaestro.manager;

import net.sf.saxon.xqj.SaxonXQDataSource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.manager.Input.Connection;
import sf.net.experimaestro.manager.xq.ParentPath;
import sf.net.experimaestro.utils.XMLUtils;
import sf.net.experimaestro.utils.log.Logger;

import javax.xml.xquery.*;
import java.lang.reflect.Constructor;

import static java.lang.String.format;

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
	 * XPMProcess the value before it can be accessed by a task to run
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

			// Construct the from expression
			String expr = connection.path;
			
			try {
				SaxonXQDataSource xqjd = new SaxonXQDataSource();
				xqjd.registerExtensionFunction(new ParentPath());
				XQConnection xqjc = xqjd.getConnection();
				XQStaticContext xqsc = xqjc.getStaticContext();
				Node item = document.getDocumentElement();
				connection.setNamespaces(xqsc);
		

				// --- If we need to dig into the output
				if (connection.from.size() > 0) {
					String exprFrom = null;
					for (int i = connection.from.size(); --i >= 0;) {
						String step = format("xp:outputs[@xp:name='%s']",
								connection.from.get(i));
						if (exprFrom == null)
							exprFrom = step;
						else
							exprFrom = step + "/" + exprFrom;
					}
					LOGGER.debug("Processing connection [%s, %s, %s]", exprFrom,
							expr, connection.to);

					XQItem xqItem = evaluateSingletonExpression(xqjc, document,
							exprFrom, xqsc);
					LOGGER.info("Item type is %s", xqItem.getItemType()
							.toString());
					item = xqItem.getNode();
					xqjc.close();

				}

				// --- Now get the value
//				XQExpression xqje = xqjc.createExpression();
//				xqje.bindNode(XQConstants.CONTEXT_ITEM, item, null);
				XQItem xqItem = evaluateSingletonExpression(xqjc, item, expr, xqsc);
				if (xqItem == null)
					continue;
				
				switch (xqItem.getItemType().getItemKind()) {
				case XQItemType.XQITEMKIND_ATOMIC:
					item = Task.wrapValue(xqItem.getAtomicValue());
					break;
				case XQItemType.XQITEMKIND_ELEMENT:
					item = xqItem.getNode();
					break;
				default:
					throw new ExperimaestroRuntimeException(
							"Cannot handle XQuery type [%s]", item);
				}

				// --- Now connects
				
				LOGGER.debug("Answer is [%s of type %d]",
						XMLUtils.toStringObject(item), item.getNodeType());
				Document newDoc = XMLUtils.newDocument();
				if (item instanceof Document)
					item = ((Document) item).getDocumentElement();
				newDoc.adoptNode(item);
				newDoc.appendChild(item);
				task.setParameter(connection.to, newDoc);

			} catch (XQException e) {
				throw new ExperimaestroRuntimeException(e,
						"Cannot evaluate XPath [%s] when connecting to [%s]",
						expr, connection.to);
			}

		}
	}

	/**
	 * Evaluate an XQuery expression that should return a single item
	 * 
	 *
     * @param xqjc The XQuery connection
     * @param xqsc The static context (useful to set namespaces)
     * @throws XQException
	 */
	static XQItem evaluateSingletonExpression(XQConnection xqjc,
                                              Node contextItem, String query, XQStaticContext xqsc) throws XQException {
		XQExpression xqje = xqjc.createExpression(xqsc);
		xqje.bindNode(XQConstants.CONTEXT_ITEM, contextItem, null);

		XQSequence result = xqje.executeQuery(query);

		if (!result.next()) {
			LOGGER.warn("No answer for XQuery [%s]", query);
			return null;
		}

		XQItem xqItem = result.getItem();
		if (result.next())
			throw new ExperimaestroRuntimeException(
					"Too many answers (%d) for XPath [%s]", query);

		return xqItem;
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
			throw new ExperimaestroRuntimeException(t);
		}
	}

	protected void init(Value other) {
		this.input = other.input;
	}

}
