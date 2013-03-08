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


import org.w3c.dom.Element;
import org.w3c.dom.Node;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.exceptions.ValueMismatchException;

/**
 * An XML type defined by a qualified name
 */
public class Type {
    private static final long serialVersionUID = 1L;

    /**
     * The qualified name of this type
     */
    private final QName qname;

    public Type(QName qname) {
        this.qname = qname;
    }

    public QName getId() {
        return qname;
    }

    public QName qname() {
        return qname;
    }

    public String getNamespaceURI() {
        return qname().getNamespaceURI();
    }

    public String getLocalPart() {
        return qname.getLocalPart();
    }

    @Override
    public String toString() {
        return qname == null ? "ANY" : qname.toString();
    }

    /**
     * REturns whether the tag name matches the type
     *
     * @param namespaceURI
     * @param name
     * @return <tt>true</tt> if it matches, false otherwise
     */
    public boolean matches(String namespaceURI, String name) {
        return namespaceURI.equals(getNamespaceURI()) && name.equals(getLocalPart());
    }

    /**
     * Validate the value
     *
     * @param node The XML node to validate
     */
    public void validate(Node node) throws ValueMismatchException {
        final Element element = (Element)node;

        if (element == null)
            throw new ExperimaestroRuntimeException("No root element");

        // Check if we have the expected element type
        final String tagName = element.getLocalName();
        String tagURI = element.getNamespaceURI();
        if (tagURI == null)
            tagURI = "";

        if (!matches(tagURI, tagName))
            throw new ValueMismatchException("Parameter was set to a value with a wrong type [{%s}%s] - expected [%s]",
                   tagURI, tagName, this);


    }

}
