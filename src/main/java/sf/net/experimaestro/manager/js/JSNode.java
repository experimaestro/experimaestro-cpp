/*
 * This file is part of experimaestro.
 * Copyright (c) 2013 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.manager.js;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.XMLUtils;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

/**
 * A wrapper around a DOM node
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 6/3/13
 */
public class JSNode extends JSBaseObject {
    private final Node node;

    public JSNode(Node node) {
        this.node = node;
    }


    @JSFunction(value = "path", scope = true)
    public JSNodeList path(Context context, Scriptable scope, String expression) throws XPathExpressionException {
        XPathExpression xpath = XMLUtils.parseXPath(expression, JSUtils.getNamespaceContext(scope));
        NodeList list = (NodeList) xpath.evaluate(node, XPathConstants.NODESET);
        return new JSNodeList(list);
    }



    @JSFunction(value = "get_string", scope = true)
    public String getString(Context context, Scriptable scope, String expression) throws XPathExpressionException {
        XPathExpression xpath = XMLUtils.parseXPath(expression, JSUtils.getNamespaceContext(scope));
        return xpath.evaluate(node);
    }

    @JSFunction(value = "get_node", scope = true)
    public Object getNode(Context context, Scriptable scope, String expression) throws XPathExpressionException {
        XPathExpression xpath = XMLUtils.parseXPath(expression, JSUtils.getNamespaceContext(scope));
        Node node = (Node) xpath.evaluate(this.node, XPathConstants.NODE);
        if (node == null)
            return NOT_FOUND;
        return new JSNode(node);
    }


    @JSFunction(value = "get_value")
    public Object getValue() throws XPathExpressionException {
        return Manager.unwrap(node);
    }

    @JSFunction(value = "get_value", scope = true)
    public Object getValue(Context context, Scriptable scope, String expression) throws XPathExpressionException {
        XPathExpression xpath = XMLUtils.parseXPath(expression, JSUtils.getNamespaceContext(scope));
        Node node = (Node) xpath.evaluate(this.node, XPathConstants.NODE);
        if (node == null)
            return NOT_FOUND;
        return Manager.unwrap(node);
    }

    @JSFunction("text")
    public String getText() {
        String text = (node instanceof Document ? ((Document)node).getDocumentElement() : node).getTextContent();
        return text == null ? "" : text;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", getClassName(), XMLUtils.getTypeName(node.getNodeType()));
    }

    @JSFunction("toSource")
    public String toSource() {
        return XMLUtils.toString(node);
    }

    public Node getNode() {
        return node;
    }
}
