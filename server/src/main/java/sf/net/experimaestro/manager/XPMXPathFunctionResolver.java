package sf.net.experimaestro.manager;

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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;
import javax.xml.xpath.XPathFunctionResolver;
import java.io.File;
import java.util.List;

import static java.lang.String.format;

/**
 * Adds some handy functions.
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class XPMXPathFunctionResolver implements XPathFunctionResolver {

    /**
     * Returns the parentpath
     */
    private static final XPathFunction ParentPath_1 = args -> new File(argToString(args.get(0))).getParentFile().toString();
    /**
     * Returns the parentpath
     */
    private static final XPathFunction JoinPath = args -> {
        File file = null;
        for (int i = 0; i < args.size(); i++) {
            String name = argToString(args.get(i));
            if (file == null)
                file = new File(name);
            else
                file = new File(file, name);
        }
        return file.getAbsolutePath();
    };
    private XPathFunctionResolver resolver;

    public XPMXPathFunctionResolver(XPathFunctionResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Converts whatever XML object into a string
     *
     * @param arg an XML object
     * @return
     * @throws XPathFunctionException
     */
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

        throw new XPathFunctionException(format(
                "Could not convert argument type [%s]",
                arg == null ? "null" : arg.getClass()));
    }

    @Override
    public XPathFunction resolveFunction(QName functionName, int arity) {
        if (Manager.EXPERIMAESTRO_NS.equals(functionName.getNamespaceURI())) {
            final String name = functionName.getLocalPart();
            if ("parentPath".equals(name) && arity == 1)
                return ParentPath_1;
            if ("joinPaths".equals(name) && arity > 1)
                return JoinPath;
        }

        return resolver == null ? null : resolver.resolveFunction(functionName,
                arity);
    }
}
