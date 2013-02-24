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

package sf.net.experimaestro.manager.plans;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.utils.XMLUtils;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 22/2/13
 */
public class XPathFunction implements Function {
    private final XPathExpression xpath;

    public XPathFunction(String xpath) throws XPathExpressionException {
        this.xpath = XMLUtils.parseXPath(xpath);
    }

    @Override
    public Node f(Node[] input) {
        assert input.length == 1;
        try {
            final NodeList list = (NodeList) xpath.evaluate(input[0], XPathConstants.NODESET);

            if (list.getLength() == 0)
                throw new ExperimaestroRuntimeException("XPath [%s] did not return any result", xpath);
            return XMLUtils.toDocumentFragment(list);

        } catch (XPathExpressionException e) {
            throw new ExperimaestroRuntimeException(e);
        }
    }
}
