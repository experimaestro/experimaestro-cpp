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

package sf.net.experimaestro.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.utils.iterators.AbstractIterator;
import sf.net.experimaestro.utils.log.Logger;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.TreeMap;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class XMLUtils {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * Convert an XML node into a string
     *
     * @param node The XML node to convert
     * @return A string representing the XML element
     */
    static public final String toString(Node node) {
        try {
            DOMImplementationRegistry registry = DOMImplementationRegistry
                    .newInstance();

            DOMImplementationLS impl = (DOMImplementationLS) registry
                    .getDOMImplementation("LS");

            LSSerializer writer = impl.createLSSerializer();
            return writer.writeToString(node);
        } catch (Exception e) {
            return e.toString();
        }
    }

    /**
     * Wrapper so that the XML is transformed into a string on demand
     *
     * @param node
     * @return
     */
    static public final Object toStringObject(final Node node) {
        return new Object() {
            private String s;

            @Override
            public String toString() {
                if (s == null)
                    s = XMLUtils.toString(node);
                return s;
            }
        };
    }

    public static String toString(NodeList nodes) {
        try {
            StringWriter stw = new StringWriter();
            Transformer serializer = TransformerFactory.newInstance()
                    .newTransformer();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node item = nodes.item(i);
                switch (item.getNodeType()) {
                    case Node.TEXT_NODE:
                        stw.append(item.getTextContent());
                        break;
                    default:
                        serializer.transform(new DOMSource(item), new StreamResult(
                                stw));
                }
            }
            return stw.toString();
        } catch (Exception e) {
            return e.toString();
        }
    }


    /**
     * Finds a child with a given qualified name
     *
     * @param element
     * @param qName
     * @return
     */
    public static Node getChild(Element element, QName qName) {
        NodeList list = element.getChildNodes();
        Element candidate = null;
        String ns = qName.getNamespaceURI();
        String name = qName.getLocalPart();
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            String nodeNS = node.getNamespaceURI();
            if (node instanceof Element && node.getLocalName().equals(name)
                    && ((ns == null && nodeNS == null) || (ns.equals(nodeNS)))) {
                if (candidate != null)
                    throw new ExperimaestroRuntimeException(
                            "Two children with the same name [%s]", qName);
                candidate = (Element) node;
            }
        }
        return candidate;
    }

    /**
     * Finds a child with a given qualified name
     *
     * @param element
     * @param qName
     * @return An iterator
     */
    public static Iterable<Element> childIterator(Element element, QName qName) {
        ArrayList<Element> x = new ArrayList<>();
        NodeList list = element.getChildNodes();
        String ns = qName.getNamespaceURI();
        String name = qName.getLocalPart();
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            String nodeNS = node.getNamespaceURI();
            if (node instanceof Element && node.getLocalName().equals(name)
                    && ((ns == null && nodeNS == null) || (ns.equals(nodeNS)))) {
                x.add((Element) node);
            }
        }
        return x;
    }

    public static Iterable<Element> childElements(final Node node) {
        return elements(children(node));
    }

    public static Iterable<? extends Node> children(Node node) {
        final NodeList nodes = node.getChildNodes();

        return nodes(nodes);
    }


    public static Iterable<Element> elements(final NodeList nodes) {
        return Iterables.filter(nodes(nodes), Element.class);
    }

    public static Iterable<Element> elements(final Iterable<? extends Node> nodes) {
        return Iterables.filter(nodes, Element.class);
    }

    private static Iterable<Node> nodes(final NodeList nodes) {
        return new Iterable<Node>() {
            @Override
            public Iterator<Node> iterator() {
                return new AbstractIterator<Node>() {
                    int i = 0;

                    @Override
                    protected boolean storeNext() {
                        if (i < nodes.getLength()) {
                            Node node = nodes.item(i++);
                            value = node;
                            return true;
                        }
                        return false;
                    }
                };
            }
        };
    }


    private final static DocumentBuilderFactory dbFactory = DocumentBuilderFactory
            .newInstance();
    private static DocumentBuilder documentBuilder;

    static {
        try {
            dbFactory.setNamespaceAware(true);
            documentBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new ExperimaestroRuntimeException(
                    "Could not build a document builder", e);
        }
    }

    /**
     * Creates a new XML document
     *
     * @return
     */
    public static Document newDocument() {
        return documentBuilder.newDocument();
    }

    /**
     * Gather all the namespaces defined on a node
     *
     * @return
     */
    public static Iterable<Entry<String, String>> getNamespaces(Element element) {
        TreeMap<String, String> map = new TreeMap<String, String>();
        do {
            NamedNodeMap attributes = element.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                Attr attr = (Attr) attributes.item(i);
                final String name = attr.getLocalName();

                if (attr.getPrefix() != null) {
                    if ("xmlns".equals(attr.getPrefix()))
                        if (!map.containsKey(name))
                            map.put(name, attr.getValue());
                } else if ("xmlns".equals(name)) {
                    if (!map.containsKey(""))
                        map.put("", attr.getValue());
                }
            }
            if (element.getParentNode() == null || element.getParentNode().getNodeType() != Node.ELEMENT_NODE)
                break;
            element = (Element) element.getParentNode();
        } while (true);
        return map.entrySet();
    }


    public static boolean is(QName qname, Element element) {
        return qname.equals(new QName(element.getNamespaceURI(), element.getLocalName()));
    }


    public static Iterable<? extends Node> iterable(final NodeList list) {
        return new Iterable<Node>() {
            @Override
            public Iterator<Node> iterator() {
                return new com.google.common.collect.AbstractIterator<Node>() {
                    int i = 0;

                    @Override
                    protected Node computeNext() {
                        if (i >= list.getLength())
                            return endOfData();

                        return list.item(i++);
                    }
                };
            }
        };
    }

    public static void cloneAndAppend(Document document, Node node) {
        node = node.cloneNode(true);
        document.adoptNode(node);
        document.appendChild(node);
    }


    public static Iterable<? extends Node> iterable(Object nodes) {
        if (nodes instanceof Node)
            return ImmutableList.of((Node) nodes);
        return iterable((NodeList) nodes);
    }

    /**
     * Returns the "root" element if it exists.
     *
     * More precisely, for types
     * <ul>
     *     <li>Document: returns the document element</li>
     *     <li>Element: returns the element</li>
     *     <li>Other: ensures there is only one child element and returns it</li>
     * </ul>
     *
     *
     * @param node
     * @return
     * @throws java.util.NoSuchElementException
     *          if no root element exists
     */
    public static Element getRootElement(Node node) throws NoSuchElementException {
        Element element = null;
        if (node instanceof Document) {
            element = ((Document) node).getDocumentElement();
        } if (node instanceof Element) {
            return (Element) node;
        } else {
            final Iterator<Element> iterator = elements(node.getChildNodes()).iterator();
            if (iterator.hasNext()) {
                element = iterator.next();
                if (iterator.hasNext())
                    throw new NoSuchElementException("Document fragment has more than one element");
            }
        }

        if (element == null)
            throw new NoSuchElementException("Document fragment has no child element");

        return element;
    }

    final static private XPathFactory XPATH_FACTORY = XPathFactory.newInstance();

    public static XPathExpression parseXPath(String path) throws XPathExpressionException {
        return XPATH_FACTORY.newXPath().compile(path);
    }

    public static XPathExpression parseXPath(String expression, NamespaceContext nsContext) throws XPathExpressionException {
        XPath xpath = XPATH_FACTORY.newXPath();
        xpath.setNamespaceContext(nsContext);
        return xpath.compile(expression);
    }

    /** Tranform a node list into a document fragment */
    public static Node toDocumentFragment(NodeList list) {
        if (list.getLength() == 1 && list.item(0) instanceof Document)
            return list.item(0);

        Document document = newDocument();
        DocumentFragment fragment = document.createDocumentFragment();
        for(int i = 0; i < list.getLength(); i++)
            fragment.appendChild(document.adoptNode(list.item(i).cloneNode(true)));
        return fragment;
    }

    /**
     * Returns the node type name from the node type code
     * @param nodeType
     * @return
     */
    public static String getTypeName(short nodeType) {
        switch (nodeType) {
            case Node.ELEMENT_NODE: return "element";
            case Node.DOCUMENT_NODE: return "document";
            case Node.TEXT_NODE: return "text";
            case Node.ATTRIBUTE_NODE: return "attribute";
            case Node.CDATA_SECTION_NODE: return "cdata";
        }
        return "Unknown[" + nodeType + "]";
    }

    /**
     * Parse a string into an XML document
     * @param s The string to parse
     * @return A valid XML document
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public static Document parseString(String s) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        return dBuilder.parse(new InputSource(new StringReader(s)));
    }
}
