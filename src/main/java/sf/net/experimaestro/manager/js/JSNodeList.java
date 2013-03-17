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

import bpiwowar.argparser.utils.Formatter;
import bpiwowar.argparser.utils.Output;
import com.google.common.collect.Lists;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import sf.net.experimaestro.exceptions.XPMRhinoException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.RangeUtils;
import sf.net.experimaestro.utils.XMLUtils;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Ranges.closed;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 7/3/13
 */
@JSHelp("A list of XML nodes")
public class JSNodeList extends JSBaseObject implements Iterable<Node> {
    private final NodeList list;

    public JSNodeList(NodeList list) {
        this.list = list;
    }

    @Override
    public Object[] getIds() {
        return RangeUtils.toIntegerArray(closed(0, list.getLength()));
    }

    @Override
    public boolean has(int index, Scriptable start) {
        return index >= 0 && index < list.getLength();
    }

    @Override
    public Object get(int index, Scriptable start) {
        return new JSNode(list.item(index));
    }

    @Override
    public Iterator<Node> iterator() {
        return (Iterator<Node>) XMLUtils.iterable(list).iterator();
    }

    @JSFunction("toSource")
    public String toSource() {
        return String.format("NodeList(%s)", Output.toString(
                ", ",
                XMLUtils.iterable(list),
                new Formatter<Node>() {
                    @Override
                    public String format(Node node) {
                        return XMLUtils.toString(node);
                    }
                }
        ));
    }


    @JSFunction(value = "get_string", scope = true)
    public String getString(Context context, Scriptable scope, String expression) throws XPathExpressionException {
        XPathExpression xpath = XMLUtils.parseXPath(expression, JSUtils.getNamespaceContext(scope));
        StringBuilder sb = new StringBuilder();
        for (Node node : XMLUtils.iterable(list)) {
            sb.append(xpath.evaluate(node));
        }

        return sb.toString();
    }

    @JSFunction(scope = true)
    public Object toE4X(Context cx, Scriptable scope) {
        Document document = XMLUtils.newDocument();
        DocumentFragment fragment = document.createDocumentFragment();
        for (Node node : XMLUtils.iterable(list))
            fragment.appendChild(document.adoptNode(node.cloneNode(true)));
        return JSUtils.domToE4X(fragment, cx, scope);
    }

    @JSFunction(scope = true, optional = 1)
    public String resource(Context cx, Scriptable scope, String expression) throws XPathExpressionException {
        List<Node> list = evaluate_xpath(scope, expression);
        if (list.size() != 1)
            throw new XPMRhinoException("XPath expression %s gave more than one result (%d)", expression, list.size());
        return JSNode.getAttribute(list.get(0), Manager.XP_RESOURCE);
    }

    public List<Node> evaluate_xpath(Scriptable scope, String expression) throws XPathExpressionException {
        if (expression == null)
            return Lists.newArrayList(XMLUtils.iterable(list));
        XPathExpression xpath = XMLUtils.parseXPath(expression, JSUtils.getNamespaceContext(scope));
        ArrayList<Node> result = new ArrayList<>();
        for (Node node : XMLUtils.iterable(list)) {
            for (Node resultNode : XMLUtils.iterable((NodeList) xpath.evaluate(node, XPathConstants.NODESET))) {
                result.add(resultNode);
            }
        }
        return result;
    }
}

