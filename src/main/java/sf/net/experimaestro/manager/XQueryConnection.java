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

import org.apache.commons.lang.NotImplementedException;
import org.w3c.dom.Element;
import sf.net.experimaestro.manager.json.Json;

import javax.xml.xquery.XQException;
import javax.xml.xquery.XQStaticContext;
import java.util.HashMap;
import java.util.Map;

/**
 * FIXME: remove ?
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 12/10/12
 */
public class XQueryConnection extends Connection {
    private Map<String, String> namespaces = new HashMap<>();

    String query;

    public XQueryConnection(DotName to, String query) {
        super(to);
        this.query = query;
    }



    public String getXQuery() {
        return query;
    }

    public void bind(String var, DotName name) {
//        inputs.add(Pair.create(var, name));
    }

    public void addNamespaces(Element element) {
        namespaces.putAll(Manager.getNamespaces(element));
    }

    /**
     * Set the defined namespaces during XQuery
     * @param xqsc
     * @throws javax.xml.xquery.XQException
     */
    public void setNamespaces(XQStaticContext xqsc) throws XQException {
        for (Map.Entry<String, String> mapping : namespaces.entrySet()) {
            Input.LOGGER.debug("Setting default namespace mapping [%s] to [%s]",
                    mapping.getKey(), mapping.getValue());
            xqsc.declareNamespace(mapping.getKey(), mapping.getValue());
        }
    }



    public boolean isRequired() {
        return required;
    }

    @Override
    public Iterable<String> inputs() {
        // TODO: implement inputs
        throw new NotImplementedException();
    }

    @Override
    public Json computeValue(Task task) {
        throw new NotImplementedException();
//            final String xQuery = connection.getXQuery();
//            final StringBuilder queryBuilder = new StringBuilder();
//
//            try {
//
//                SaxonXQDataSource xqjd = new SaxonXQDataSource();
//                xqjd.registerExtensionFunction(new ParentPath());
//                XQConnection xqjc = xqjd.getConnection();
//                XQStaticContext xqsc = xqjc.getStaticContext();
//                connection.setNamespaces(xqsc);
//                final XQExpression xqje = xqjc.createExpression(xqsc);
//
//                // Collect the inputs and set the appropriate context
//                for (Map.Entry<String, DotName> pair : connection.getInputs()) {
//                    final String varName = pair.getKey();
//                    final DotName from = pair.getValue();
//
//                    final Value value = task.getValues().get(from.get(0));
//                    final Json document = value.get();
//
//                    // Search for the appropriate output
//
//                    if (document == null) {
//                        // If this input was not set, bind to empty sequence
//                        LOGGER.debug("Binding $%s to empty sequence", varName);
//                        queryBuilder.append("declare variable $" + varName + " := (); ");
//                    } else {
//                        Value bindValue = task.getValue(from);
//                        Json element = bindValue.get();
//                        LOGGER.debug("Binding $%s to element [%s]", varName, ((Element) element).getTagName());
//                        LOGGER.debug(element);
//                        queryBuilder.append("declare variable $" + varName + " external; ");
//
//                        xqje.bindNode(new javax.xml.namespace.QName(varName), element, null);
//                    }
//                }
//
//
//                // --- Evaluate the XQuery
//                queryBuilder.append(xQuery);
//                final String query = queryBuilder.toString();
//                LOGGER.debug("Evaluated XQuery is %s", query);
//                XQItem xqItem = evaluateSingletonExpression(query, xqje);
//                if (xqItem == null) {
//                    if (connection.isRequired())
//                        throw new ExperimaestroRuntimeException("Could not connect");
//                    continue;
//                }
//
//                Value destination = task.getValue(connection.to);
//                Node item;
//                final int itemKind = xqItem.getItemType().getItemKind();
//                switch (itemKind) {
//                    case XQItemType.XQITEMKIND_ATOMIC:
//                        item = ValueType.wrapString(xqItem.getAtomicValue(), null);
//                        break;
//                    case XQItemType.XQITEMKIND_ELEMENT:
//                        item = xqItem.getNode();
//                        break;
//                    case XQItemType.XQITEMKIND_ATTRIBUTE:
//                    case XQItemType.XQITEMKIND_TEXT:
//                        item = ValueType.wrapString(xqItem.getNode().getTextContent(), null);
//                        break;
//                    default:
//                        throw new ExperimaestroRuntimeException(
//                                "Cannot handle XQuery type [%s]", xqItem.getItemType());
//                }
//
//                // --- Now connects
//
//                LOGGER.debug("Answer is [%s of type %d]",
//                        XMLUtils.toStringObject(item), item.getNodeType());
//                Document newDoc = XMLUtils.newDocument();
//                if (item instanceof Document)
//                    item = ((Document) item).getDocumentElement();
//
//                item = item.cloneNode(true);
//                newDoc.adoptNode(item);
//                newDoc.appendChild(item);
//                LOGGER.debug("Setting parameter [%s] in [%s]", connection.to, task);
//                destination.set(newDoc);
//                xqjc.close();
//
//            } catch (XQException e) {
//                final ExperimaestroRuntimeException exception = new ExperimaestroRuntimeException(e,
//                        "Cannot evaluate XPath [%s] when connecting to [%s]", xQuery, connection.to);
//                exception.addContext("Internal error: " + e.toString());
//                throw exception;
//            }
    }
}
