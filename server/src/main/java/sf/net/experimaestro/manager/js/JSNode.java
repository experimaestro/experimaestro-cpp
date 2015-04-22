package sf.net.experimaestro.manager.js;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
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

import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang.NotImplementedException;
import org.json.simple.JSONValue;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sf.net.experimaestro.exceptions.XPMRhinoException;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.ValueType;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonWriterOptions;
import sf.net.experimaestro.manager.scripting.Expose;
import sf.net.experimaestro.utils.JSNamespaceContext;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.XMLUtils;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.FileSystemException;

/**
 * A wrapper around a DOM node
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 6/3/13
 */
public class JSNode extends JSBaseObject implements Json {
    public static final Charset UTF16 = Charset.forName("UTF-16");
    private final Node node;

    public JSNode(Node node) {
        this.node = node;
    }

    static String getAttribute(Node node, QName attributeQName) {
        Element element = node instanceof Document ? ((Document) node).getDocumentElement() : (Element) node;
        if (!attributeQName.isAttribute(element)) {
            throw new XPMRhinoException("No " + attributeQName + " associated to XML element %s", new QName(element));
        }

        return attributeQName.getAttribute(element);
    }

    @Expose(value = "xpath", scope = true)
    public JSNodeList xpath(Context context, Scriptable scope, String expression) throws XPathExpressionException {
        NodeList list = evaluate(scope, expression);
        return new JSNodeList(list);
    }

    private NodeList evaluate(Scriptable scope, String expression) throws XPathExpressionException {
        XPathExpression xpath = XMLUtils.parseXPath(expression, JSUtils.getNamespaceContext(scope));
        return (NodeList) xpath.evaluate(node, XPathConstants.NODESET);
    }

    @Expose(value = "get_string", scope = true)
    public String getString(Context context, Scriptable scope, String expression) throws XPathExpressionException {
        XPathExpression xpath = XMLUtils.parseXPath(expression, JSUtils.getNamespaceContext(scope));
        return xpath.evaluate(node);
    }

    @Expose(value = "get_string")
    public String getString() {
        return node.getTextContent();
    }

    @Expose(value = "get_node", scope = true)
    public Object getNode(Context context, Scriptable scope, String expression) throws XPathExpressionException {
        XPathExpression xpath = XMLUtils.parseXPath(expression, JSUtils.getNamespaceContext(scope));
        Node node = (Node) xpath.evaluate(this.node, XPathConstants.NODE);
        if (node == null)
            return NOT_FOUND;
        return new JSNode(node);
    }

    @Expose("text")
    public String getText() {
        if (node == null)
            return "";
        String text = getElement().getTextContent();
        return text == null ? "" : text;
    }

    @Expose(value = "text", scope = true)
    public String getText(Context context, Scriptable scope, String expression) throws XPathExpressionException {
        XPathExpression xpath = XMLUtils.parseXPath(expression, JSUtils.getNamespaceContext(scope));
        Node node = (Node) xpath.evaluate(this.node, XPathConstants.NODE);
        if (node == null)
            return "";

        String text = node.getTextContent();
        return text == null ? "" : text;
    }

    private Element getElement() {
        return node instanceof Document ? ((Document) node).getDocumentElement() : (Element) node;
    }

    @Expose(scope = true)
    public Object toE4X(Context cx, Scriptable scope) {
        return JSUtils.domToE4X(node, cx, scope);
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", getClassName(), node == null ? "[null]" : XMLUtils.getTypeName(node.getNodeType()));
    }

    @Expose("toSource")
    public String toSource() {
        return XMLUtils.toString(node, false);
    }

    @Expose("toSource")
    public String toSource(boolean declaration) {
        return XMLUtils.toString(node, declaration);
    }

    @Override
    public byte[] getBytes() {
        return XMLUtils.toString(node).getBytes(UTF16);
    }

    @Expose()
    public String resource() {
        return getAttribute(node, ValueType.XP_RESOURCE);
    }

    @Expose(scope = true)
    public String resource(Context cx, Scriptable scope, String xpath) throws XPathExpressionException {
        NodeList nodeList = get_one_node(scope, xpath);
        return getAttribute(nodeList.item(0), ValueType.XP_RESOURCE);
    }

    @Expose(scope = true)
    public JSPath path(Context cx, Scriptable scope) throws FileSystemException {
        XPMObject xpm = XPMObject.getXPMObject(scope);
        return new JSPath(getAttribute(node, ValueType.XP_PATH));

    }

    @Expose(scope = true)
    public JSPath path(Context cx, Scriptable scope, String xpath) throws XPathExpressionException, FileSystemException {
        NodeList nodeList = get_one_node(scope, xpath);
        return new JSPath(getAttribute(nodeList.item(0), ValueType.XP_PATH));
    }

    @Expose(scope = true)
    public void set_attribute(Context cx, Scriptable scope, String name, String value) {
        QName qname = QName.parse(name, new JSNamespaceContext(scope));
        getElement().setAttributeNS(qname.getNamespaceURI(), qname.getLocalPart(), value);
    }

    private NodeList get_one_node(Scriptable scope, String xpath) throws XPathExpressionException {
        NodeList nodeList = evaluate(scope, xpath);
        if (nodeList.getLength() != 1)
            throw new XPMRhinoException("XPath expression %s gave more than one result (%d)", xpath, nodeList.getLength());
        return nodeList;
    }

    public Node getNode() {
        return node;
    }

    @Override
    public Json clone() {
        return new JSNode(node);
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public Object get() {
        return node;
    }

    @Override
    public QName type() {
        return ValueType.XP_XML;
    }

    @Override
    public boolean
    canIgnore(JsonWriterOptions options) {
        return false;
    }

    @Override
    public void writeDescriptorString(Writer writer, JsonWriterOptions options) throws IOException {
        write(writer);
    }

    @Override
    public void write(Writer out) throws IOException {
        out.write(JSONValue.escape(XMLUtils.toString(node)));
    }

    @Override
    public void write(JsonWriter out) {
        throw new NotImplementedException();
    }
}
