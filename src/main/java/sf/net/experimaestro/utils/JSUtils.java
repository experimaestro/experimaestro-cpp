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

import org.mozilla.javascript.*;
import org.mozilla.javascript.xml.XMLObject;
import org.mozilla.javascript.xmlimpl.XMLLibImpl;
import org.w3c.dom.*;
import org.w3c.dom.Node;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.js.JSNamespaceBinder;
import sf.net.experimaestro.utils.log.Logger;

import static java.lang.String.format;

public class JSUtils {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * Get an object from a scriptable
     *
     * @param object
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(Scriptable scope, String name, NativeObject object, boolean allowNull) {
        final Object _value = object.get(name, scope);
        if (_value == UniqueTag.NOT_FOUND)
            if (allowNull) return null;
            else throw new RuntimeException(format("Could not find property '%s'",
                    name));
        return (T) unwrap(_value);
    }

    public static <T> T get(Scriptable scope, String name, NativeObject object) {
        return get(scope, name, object, false);
    }


    /**
     * Unwrap a JavaScript object (if necessary)
     *
     * @param object
     * @return
     */
    public static Object unwrap(Object object) {
        if (object == null)
            return null;

        if (object instanceof Wrapper)
            object = ((Wrapper) object).unwrap();

        return object;
    }

    /**
     * Get an object from a scriptable
     *
     * @param object
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(Scriptable scope, String name, NativeObject object,
                            T defaultValue) {
        final Object _value = object.get(name, scope);
        if (_value == UniqueTag.NOT_FOUND)
            return defaultValue;
        return (T) unwrap(_value);
    }

    /**
     * Transforms a DOM node to a E4X scriptable object
     *
     * @param node
     * @param cx
     * @param scope
     * @return
     */
    public static Object domToE4X(Node node, Context cx, Scriptable scope) {
        if (node == null) {
            LOGGER.info("XML is null");
            return Context.getUndefinedValue();
        }
        if (node instanceof Document)
            node = ((Document) node).getDocumentElement();

        if (node instanceof DocumentFragment) {

            final Document document = node.getOwnerDocument();
            Element root = document.createElement("root");
            document.appendChild(root);

            Scriptable xml = cx.newObject(scope, "XML", new Node[]{root});

            final Scriptable list = (Scriptable) xml.get("*", xml);
            for (Node child : XMLUtils.children(node)) {
                list.put(0, list, cx.newObject(scope, "XML", new Node[]{child}));
            }
            return list;
        }

        LOGGER.debug("XML is of type %s [%s]; %s", node.getClass(),
                XMLUtils.toStringObject(node),
                node.getUserData("org.mozilla.javascript.xmlimpl.XmlNode"));
        return cx.newObject(scope, "XML", new Node[]{node});
    }

    /**
     * Transform objects into an XML node
     *
     * @param object
     * @return a {@linkplain Node} or a {@linkplain NodeList}
     */
    public static Node toDOM(Scriptable scope, Object object) {
        // Unwrap if needed
        if (object instanceof Wrapper)
            object = ((Wrapper) object).unwrap();

        // It is already a DOM node
        if (object instanceof Node)
            return (Node) object;

        if (object instanceof XMLObject) {
            final XMLObject xmlObject = (XMLObject) object;
            String className = xmlObject.getClassName();

            if (className.equals("XMLList")) {
                LOGGER.debug("Transforming from XMLList [%s]", object);
                final Object[] ids = xmlObject.getIds();
                if (ids.length == 1)
                    return toDOM(scope, xmlObject.get((Integer) ids[0], xmlObject));

                Document doc = XMLUtils.newDocument();
                DocumentFragment fragment = doc.createDocumentFragment();

                for (int i = 0; i < ids.length; i++) {
                    Node node = toDOM(scope, xmlObject.get((Integer) ids[i], xmlObject));
                    if (node instanceof Document)
                        node = ((Document) node).getDocumentElement();
                    fragment.appendChild(doc.adoptNode(node));
                }

                return fragment;
            }

            // XML node
            if (className.equals("XML")) {
                // FIXME: this strips all whitespaces!
                Node node = XMLLibImpl.toDomNode(object);
                LOGGER.debug("Got node from JavaScript [%s / %s] from [%s]",
                        node.getClass(), XMLUtils.toStringObject(node),
                        object.toString());

                if (node instanceof Document)
                    node = ((Document) node).getDocumentElement();

                final Document document = XMLUtils.newDocument();
                node = document.adoptNode(node.cloneNode(true));
                if (node instanceof Element) {
                    document.appendChild(node);
                    return document;
                }
                final DocumentFragment fragment = document.createDocumentFragment();
                fragment.appendChild(node);
                return fragment;
            }


            throw new RuntimeException(format(
                    "Not implemented: convert %s to XML", className));

        }

        if (object instanceof NativeObject) {
            // JSON case: each key of the JS object is an XML element
            NativeObject json = (NativeObject) object;
            Document document = XMLUtils.newDocument();
            DocumentFragment fragment = document.createDocumentFragment();

            for (Object _id : json.getIds()) {

                final QName qname = QName.parse(JSUtils.toString(_id), null, new JSNamespaceBinder(scope));

                Element element = qname.hasNamespace() ?
                        document.createElementNS(qname.getNamespaceURI(), qname.getLocalPart())
                        : document.createElement(qname.getLocalPart());

                fragment.appendChild(element);

                final Object seq = toDOM(scope, json.get(JSUtils.toString(_id), json));
                for (Node node : XMLUtils.iterable(seq)) {
                    node = node.cloneNode(true);
                    element.appendChild(document.adoptNode(node));
                }
            }

            return fragment;
        }

        if (object instanceof Double) {
            // Wrap a double
            final Double x = (Double) object;
            if (x.longValue() == x.doubleValue())
                return wrap(Manager.EXPERIMAESTRO_NS, "value", Long.toString(x.longValue()));
            return wrap(Manager.EXPERIMAESTRO_NS, "value", Double.toString(x));
        }

        if (object instanceof Integer) {
            return wrap(Manager.EXPERIMAESTRO_NS, "value", Integer.toString((Integer) object));
        }

        throw new ExperimaestroRuntimeException("Class %s cannot be converted to XML", object.getClass());
    }

    /**
     * Returns true if the object is XML
     *
     * @param input
     * @return
     */
    public static boolean isXML(Object input) {
        return input instanceof XMLObject;
    }

    /**
     * Converts a JavaScript object into an XML document
     *
     * @param object
     * @param wrapName If the object is not already a document and has more than one
     *                 element child (or zero), use this to wrap the elements
     * @return
     */
    public static Document toDocument(Scriptable scope, Object object, QName wrapName) {
        final Node dom = toDOM(scope, object);

        if (dom instanceof Document)
            return (Document) dom;

        Document document = XMLUtils.newDocument();

        // Add a new root element if needed
        NodeList childNodes;


        if (dom.getNodeType() == Node.ELEMENT_NODE) {
            childNodes = new NodeList() {
                @Override
                public Node item(int index) {
                    if (index == 0)
                        return dom;
                    throw new IndexOutOfBoundsException(Integer.toString(index) + " out of bounds");
                }

                @Override
                public int getLength() {
                    return 1;
                }
            };
        } else
            childNodes = dom.getChildNodes();

        int elementCount = 0;
        for (int i = 0; i < childNodes.getLength(); i++)
            if (childNodes.item(i).getNodeType() == Node.ELEMENT_NODE)
                elementCount++;

        Node root = document;
        if (elementCount != 1) {
            root = document.createElementNS(wrapName.getNamespaceURI(),
                    wrapName.getLocalPart());
            document.appendChild(root);
        }

        // Copy back in the DOM
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            node = node.cloneNode(true);
            document.adoptNode(node);
            root.appendChild(node);
        }

        return document;
    }

    /**
     * Add a new javascript function to the scope
     *
     * @param aClass    The class where the function should be searched
     * @param scope     The scope where the function should be defined
     * @param fname     The function name
     * @param prototype The prototype or null. If null, uses the standard Context, Scriptablem Object[], Function prototype
     *                  used by Rhino JS
     * @throws NoSuchMethodException If
     */
    public static void addFunction(Class<?> aClass, Scriptable scope, final String fname,
                                   Class<?>[] prototype) throws NoSuchMethodException {
        final FunctionObject f = new FunctionObject(fname,
                aClass.getMethod("js_" + fname, prototype), scope);
        ScriptableObject.putProperty(scope, fname, f);
    }

    public static String toString(Object object) {
        return Context.toString(unwrap(object));
    }

    public static int getInteger(Object object) {
        return (Integer) unwrap(object);
    }

    public static void addFunction(Scriptable scope, FunctionDefinition definition) throws NoSuchMethodException {
        addFunction(definition.clazz, scope, definition.name, definition.arguments);
    }

    /**
     * Wraps a value into an XML document
     *
     * @param name
     * @param value
     * @return An XML document representing the value
     */
    static public Document wrap(String namespace, String name, String value) {
        final Document doc = XMLUtils.newDocument();
        Element element = doc.createElementNS(namespace, name);
        element.setTextContent(value);
        doc.appendChild(element);
        return doc;
    }

    /**
     * Defines a JavaScript function by refering a class, a name and its parameters
     */
    static public class FunctionDefinition {
        Class<?> clazz;
        String name;
        Class<?>[] arguments;

        public FunctionDefinition(Class<?> clazz, String name, Class<?>... arguments) {
            this.clazz = clazz;
            this.name = name;
            if (arguments == null)
                this.arguments = new Class[]{Context.class, Scriptable.class, Object[].class, Function.class};
            else
                this.arguments = arguments;
        }

        public Class<?> getClazz() {
            return clazz;
        }

        public String getName() {
            return name;
        }

        public Class<?>[] getArguments() {
            return arguments;
        }
    }

}
