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
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.exceptions.NoSuchParameter;
import sf.net.experimaestro.manager.xq.ParentPath;
import sf.net.experimaestro.utils.XMLUtils;
import sf.net.experimaestro.utils.log.Logger;

import javax.xml.namespace.QName;
import javax.xml.xquery.*;
import java.lang.reflect.Constructor;
import java.util.Map;

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
     * Returns the value object corresponding to this path
     * @param id The ID
     * @return The value or
     */
    public abstract Value getValue(DotName id) throws NoSuchParameter;


    /** Set to the given value */
    public abstract void set(Document value);

    /**
     * XPMProcess the value before it can be accessed by a task to run
     */
    public abstract void process() throws NoSuchParameter;

    /**
     * Get the value
     * <p/>
     * This method is called by a {@link Task} after {@link #process()}.
     *
     * @return A valid XML document or null if not set
     */
    public abstract Document get();

    /**
     * This method is called once by a {@link Task} after {@link #process()}.
     *
     * @param task
     */
    void processConnections(Task task) throws NoSuchParameter {
        LOGGER.debug("Processing %d connections for [%s]", input.connections.size(), task.factory.getId());
        // Do not process if we do not have connections...
        for (Connection connection : input.connections) {
            LOGGER.debug("Processing connection [%s]", connection);
            final String xQuery = connection.getXQuery();
            final StringBuilder queryBuilder = new StringBuilder();

            try {

                SaxonXQDataSource xqjd = new SaxonXQDataSource();
                xqjd.registerExtensionFunction(new ParentPath());
                XQConnection xqjc = xqjd.getConnection();
                XQStaticContext xqsc = xqjc.getStaticContext();
                connection.setNamespaces(xqsc);
                final XQExpression xqje = xqjc.createExpression(xqsc);

                // Collect the inputs and set the appropriate context
                for (Map.Entry<String, DotName> pair : connection.getInputs()) {
                    final String varName = pair.getKey();
                    final DotName from = pair.getValue();

                    final Value value = task.getValues().get(from.get(0));
                    final Document document = value.get();

                    // Search for the appropriate output

                    if (document == null) {
                        // If this input was not set, bind to empty sequence
                        LOGGER.debug("Binding $%s to empty sequence", varName);
                        queryBuilder.append("declare variable $" + varName + " := (); ");
                    } else {
                        Element element = document.getDocumentElement();
                        for (int i = 1; i < from.size(); i++) {
                            boolean found = false;
                            for (Element child : XMLUtils.childElements(element)) {
                                final String name = child.getAttributeNS(Manager.EXPERIMAESTRO_NS, "name");
                                if (name != null && name.equals(from.get(i))) {
                                    element = child;
                                    found = true;
                                }
                            }
                            if (!found)
                                throw new ExperimaestroRuntimeException("Cannot process %s in [%s]", from.get(i), from);

                        }

                        LOGGER.debug("Binding $%s to element [%s]", varName, element.getTagName());
                        LOGGER.debug(XMLUtils.toString(element));
                        queryBuilder.append("declare variable $" + varName + " external; ");

                        xqje.bindNode(new QName(varName), element, null);
                    }
                }


                // --- Evaluate the XQuery
                queryBuilder.append(xQuery);
                final String query = queryBuilder.toString();
                LOGGER.debug("Evaluated XQuery is %s", query);
                XQItem xqItem = evaluateSingletonExpression(query, xqje);
                if (xqItem == null) {
                    if (connection.isRequired())
                        throw new ExperimaestroRuntimeException("Could not connect");
                    continue;
                }

                Value destination = task.getValue(connection.to);
                Node item;
                final int itemKind = xqItem.getItemType().getItemKind();
                switch (itemKind) {
                    case XQItemType.XQITEMKIND_ATOMIC:
                        item = Task.wrapValue(destination.input.getNamespace(), connection.to.getName(), xqItem.getAtomicValue());
                        break;
                    case XQItemType.XQITEMKIND_ELEMENT:
                        item = xqItem.getNode();
                        break;
                    case XQItemType.XQITEMKIND_ATTRIBUTE:
                    case XQItemType.XQITEMKIND_TEXT:
                        item = Task.wrapValue(destination.input.getNamespace(), connection.to.getName(), xqItem.getNode().getTextContent());
                        break;
                    default:
                        throw new ExperimaestroRuntimeException(
                                "Cannot handle XQuery type [%s]", xqItem.getItemType());
                }

                // --- Now connects

                LOGGER.debug("Answer is [%s of type %d]",
                        XMLUtils.toStringObject(item), item.getNodeType());
                Document newDoc = XMLUtils.newDocument();
                if (item instanceof Document)
                    item = ((Document) item).getDocumentElement();

                item = item.cloneNode(true);
                newDoc.adoptNode(item);
                newDoc.appendChild(item);
                LOGGER.debug("Setting parameter [%s] in [%s]", connection.to, task);
                destination.set(newDoc);
                xqjc.close();

            } catch (XQException e) {
                final ExperimaestroRuntimeException exception = new ExperimaestroRuntimeException(e,
                        "Cannot evaluate XPath [%s] when connecting to [%s]", xQuery, connection.to);
                exception.addContext("Internal error: " + e.toString());
                throw exception;
            }
        }
    }

    private static XQItem evaluateSingletonExpression(String query, XQExpression xqje) throws XQException {
        XQSequence result = xqje.executeQuery(query);

        if (!result.next()) {
            LOGGER.debug("No answer for XQuery [%s]", query);
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
                    .getConstructor(new Class<?>[]{});
            Value copy = constructor.newInstance(new Object[]{});
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

    /**
     * Checks whether the value was set
     */
    public boolean isSet() {
        return get() != null;
    }


}
