package sf.net.experimaestro.utils;

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

import com.google.common.collect.AbstractIterator;
import org.mozilla.javascript.*;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.xml.XMLObject;
import org.mozilla.javascript.xmlimpl.XMLLibImpl;
import org.w3c.dom.*;
import org.w3c.dom.Node;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.js.JSBaseObject;
import sf.net.experimaestro.manager.js.JSNamespaceBinder;
import sf.net.experimaestro.manager.json.*;
import sf.net.experimaestro.manager.scripting.*;
import sf.net.experimaestro.scheduler.Resource;
import sf.net.experimaestro.utils.log.Logger;

import java.lang.reflect.Array;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        if (object == Scriptable.NOT_FOUND)
            return null;
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
            int count = 0;
            for (Node child : XMLUtils.children(node)) {
                list.put(count++, list, cx.newObject(scope, "XML", new Node[]{child}));
            }
            return list;
        }

        LOGGER.debug("XML is of type %s [%s]; %s", node.getClass(),
                XMLUtils.toStringObject(node),
                node.getUserData("org.mozilla.javascript.xmlimpl.XmlNode"));
        return cx.newObject(scope, "XML", new Node[]{node});
    }


    public static Object get(Scriptable scope, String name) {
        Scriptable start = scope;
        Object result;
        Scriptable currentScope = scope;
        do {
            scope = currentScope;
            do {
                result = scope.get(name, start);
                if (result != Scriptable.NOT_FOUND)
                    break;
                scope = scope.getPrototype();
            } while (scope != null);
            currentScope = currentScope.getParentScope();
        } while (currentScope != null);

        return result;
    }


    /**
     * @see org.mozilla.javascript.RhinoException#getScriptStack()
     * @param ex
     * @return A stack
     */
    static public ScriptStackElement[] getScriptStackTrace(Throwable ex) {
        List<ScriptStackElement> list = new ArrayList<>();
        ScriptStackElement[][] interpreterStack = null;
        int interpreterStackIndex = 0;
        StackTraceElement[] stack = ex.getStackTrace();
        // Pattern to recover function name from java method name -
        // see Codegen.getBodyMethodName()
        // kudos to Marc Guillemot for coming up with this
        Pattern pattern = Pattern.compile("_c_(.*)_\\d+");
        for (StackTraceElement e : stack) {
            String fileName = e.getFileName();
            if (e.getMethodName().startsWith("_c_")
                    && e.getLineNumber() > -1
                    && fileName != null
                    && !fileName.endsWith(".java")) {
                String methodName = e.getMethodName();
                Matcher match = pattern.matcher(methodName);
                // the method representing the main script is always "_c_script_0" -
                // at least we hope so
                methodName = !"_c_script_0".equals(methodName) && match.find() ?
                        match.group(1) : null;
                list.add(new ScriptStackElement(fileName, methodName, e.getLineNumber()));
            } else if ("org.mozilla.javascript.Interpreter".equals(e.getClassName())
                    && "interpretLoop".equals(e.getMethodName())
                    && interpreterStack != null
                    && interpreterStack.length > interpreterStackIndex) {
                for (ScriptStackElement elem : interpreterStack[interpreterStackIndex++]) {
                    list.add(elem);
                }
            }
        }
        return list.toArray(new ScriptStackElement[list.size()]);

    }

    /**
     * Transform an object to JSON
     *
     * @param scope The javascript scope for evaluating expressions
     * @param value The object to transform
     * @return
     */
    public static Json toJSON(Scriptable scope, Object value) {
        if (value instanceof sf.net.experimaestro.manager.scripting.Wrapper) {
            value = ((sf.net.experimaestro.manager.scripting.Wrapper) value).unwrap();
        }

        if (value instanceof Json)
            return (Json) value;

        // No unwrap for JSBaseObject
        value = value instanceof JSBaseObject ? value : unwrap(value);

        // --- Simple cases
        if (value == null)
            return JsonNull.getSingleton();

        if (value instanceof Json)
            return (Json) value;

        if (value instanceof String)
            return new JsonString((String) value);

        if (value instanceof Double) {
            if ((double) ((Double) value).longValue() == (double) value)
                return new JsonInteger(((Double) value).longValue());
            return new JsonReal((Double) value);
        }
        if (value instanceof Float) {
            if ((double) ((Float) value).longValue() == (float) value)
                return new JsonInteger(((Float) value).longValue());
            return new JsonReal((Float) value);
        }

        if (value instanceof Integer)
            return new JsonInteger((Integer) value);

        if (value instanceof Long)
            return new JsonInteger((Long) value);

        if (value instanceof Boolean)
            return new JsonBoolean((Boolean) value);

        // --- A JS object
        if (value instanceof NativeObject) {
            JsonObject json = new JsonObject();
            for (Map.Entry<Object, Object> entry : JSUtils.iterable(((NativeObject) value))) {
                JSNamespaceContext nsContext = new JSNamespaceContext(scope);
                QName qname = QName.parse(JSUtils.toString(entry.getKey()), nsContext);
                Object pValue = entry.getValue();

                if (qname.equals(Manager.XP_TYPE))
                    pValue = QName.parse(JSUtils.toString(pValue), nsContext).toString();

                String key = qname.toString();
                final Json key_value = toJSON(scope, pValue);
                json.put(key, key_value);
            }
            return json;
        }

        // -- An array
        if (value instanceof NativeArray) {
            NativeArray array = (NativeArray) value;
            JsonArray json = new JsonArray();
            for (int i = 0; i < array.getLength(); i++)
                json.add(toJSON(scope, array.get(i)));
            return json;
        }

        if (value.getClass().isArray()) {
            final int length = Array.getLength(value);
            JsonArray json = new JsonArray();
            for (int i = 0; i < length; i++)
                json.add(toJSON(scope, Array.get(value, i)));
            return json;
        }

        if (value instanceof java.nio.file.Path)
            return new JsonPath((java.nio.file.Path) value);

        if (value instanceof ScriptingPath)
            return new JsonPath(((ScriptingPath) value).getObject());

        if (value instanceof Resource)
            return new JsonResource((Resource) value);

        // -- Undefined
        if (value instanceof Undefined)
            return JsonNull.getSingleton();

        return new JsonString(value.toString());
    }

    private static Iterable<? extends Map.Entry<Object, Object>> iterable(final NativeObject object) {
        final Object[] ids = object.getIds();
        return () -> new AbstractIterator<Map.Entry<Object, Object>>() {
            int i = 0;

            @Override
            protected Map.Entry<Object, Object> computeNext() {
                if (i >= ids.length)
                    return endOfData();
                final Object id = ids[i++];
                String key = id.toString();
                final Object value = object.get(id);
                return new AbstractMap.SimpleImmutableEntry<>(key, value);
            }
        };
    }

    /**
     * Returns true if the object is a well defined JavaScript/Java object
     *
     * @param object
     * @return
     */
    public static boolean isDefined(Object object) {
        return object != null && object != UniqueTag.NOT_FOUND;
    }

    /**
     * Transform objects into an XML node or a NodeLsit
     *
     * @param object
     * @return a {@linkplain Node} or a {@linkplain NodeList}
     */
    public static Object toDOM(Scriptable scope, Object object) {
        return toDOM(scope, object, new OptionalDocument());
    }

    public static Object toDOM(Scriptable scope, Object object, OptionalDocument document) {
        // Unwrap if needed (if this is not a JSBaseObject)
        if (object instanceof Wrapper && !(object instanceof JSBaseObject))
            object = ((Wrapper) object).unwrap();

        // It is already a DOM node
        if (object instanceof Node)
            return object;

        if (object instanceof XMLObject) {
            final XMLObject xmlObject = (XMLObject) object;
            String className = xmlObject.getClassName();

            if (className.equals("XMLList")) {
                LOGGER.debug("Transforming from XMLList [%s]", object);
                final Object[] ids = xmlObject.getIds();
                if (ids.length == 1)
                    return toDOM(scope, xmlObject.get((Integer) ids[0], xmlObject), document);

                Document doc = XMLUtils.newDocument();
                DocumentFragment fragment = doc.createDocumentFragment();

                for (int i = 0; i < ids.length; i++) {
                    Node node = (Node) toDOM(scope, xmlObject.get((Integer) ids[i], xmlObject), document);
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

                node = document.get().adoptNode(node.cloneNode(true));
                return node;
            }


            throw new RuntimeException(format(
                    "Not implemented: convert %s to XML", className));

        }

        if (object instanceof NativeArray) {
            NativeArray array = (NativeArray) object;
            ArrayNodeList list = new ArrayNodeList();
            for (Object x : array) {
                Object o = toDOM(scope, x, document);
                if (o instanceof Node)
                    list.add(document.cloneAndAdopt((Node) o));
                else {
                    for (Node node : XMLUtils.iterable((NodeList) o)) {
                        list.add(document.cloneAndAdopt(node));
                    }
                }
            }
            return list;
        }

        if (object instanceof NativeObject) {
            // JSON case: each key of the JS object is an XML element
            NativeObject json = (NativeObject) object;
            ArrayNodeList list = new ArrayNodeList();

            for (Object _id : json.getIds()) {

                String jsQName = JSUtils.toString(_id);

                if (jsQName.length() == 0) {
                    final Object seq = toDOM(scope, json.get(jsQName, json), document);
                    for (Node node : XMLUtils.iterable(seq)) {
                        if (node instanceof Document)
                            node = ((Document) node).getDocumentElement();
                        list.add(document.cloneAndAdopt(node));
                    }
                } else if (jsQName.charAt(0) == '@') {
                    final QName qname = QName.parse(jsQName.substring(1), null, new JSNamespaceBinder(scope));
                    Attr attribute = document.get().createAttributeNS(qname.getNamespaceURI(), qname.getLocalPart());
                    StringBuilder sb = new StringBuilder();
                    for (Node node : XMLUtils.iterable(toDOM(scope, json.get(jsQName, json), document))) {
                        sb.append(node.getTextContent());
                    }

                    attribute.setTextContent(sb.toString());
                    list.add(attribute);
                } else {
                    final QName qname = QName.parse(jsQName, null, new JSNamespaceBinder(scope));
                    Element element = qname.hasNamespace() ?
                            document.get().createElementNS(qname.getNamespaceURI(), qname.getLocalPart())
                            : document.get().createElement(qname.getLocalPart());

                    list.add(element);

                    final Object seq = toDOM(scope, json.get(jsQName, json), document);
                    for (Node node : XMLUtils.iterable(seq)) {
                        if (node instanceof Document)
                            node = ((Document) node).getDocumentElement();
                        node = document.get().adoptNode(node.cloneNode(true));
                        if (node.getNodeType() == Node.ATTRIBUTE_NODE)
                            element.setAttributeNodeNS((Attr) node);
                        else
                            element.appendChild(node);
                    }
                }
            }

            return list;
        }

        if (object instanceof Double) {
            // Wrap a double
            final Double x = (Double) object;
            if (x.longValue() == x.doubleValue())
                return document.get().createTextNode(Long.toString(x.longValue()));
            return document.get().createTextNode(Double.toString(x));
        }

        if (object instanceof Integer) {
            return document.get().createTextNode(Integer.toString((Integer) object));
        }

        if (object instanceof CharSequence) {
            return document.get().createTextNode(object.toString());
        }


        if (object instanceof UniqueTag)
            throw new XPMRuntimeException("Undefined cannot be converted to XML", object.getClass());

        if (object instanceof Scriptable) {
            ((Scriptable) object).getDefaultValue(String.class);
        }

        // By default, convert to string
        return document.get().createTextNode(object.toString());
    }


    public static String toString(Object object) {
        return Context.toString(unwrap(object));
    }

    /**
     * Convert a property into a boolean
     *
     * @param scope  The JS scope
     * @param object The JS object
     * @param name   The name of the property
     * @return <tt>false</tt> if the property does not exist.
     */
    public static boolean toBoolean(Scriptable scope, Scriptable object, String name) {
        if (!object.has(name, scope)) return false;
        Object value = object.get(name, scope);
        if (value instanceof Boolean)
            return (Boolean) value;
        return Boolean.parseBoolean(JSUtils.toString(value));
    }

    public static String getString(Scriptable scope, String name, NativeObject object) {
        return toString(get(scope, name, object));
    }

    static public class OptionalDocument {
        Document document;

        Document get() {
            if (document == null)
                document = XMLUtils.newDocument();
            return document;
        }

        public boolean has() {
            return document != null;
        }

        /**
         * Clone and adopt node if not already owned
         *
         * @param node
         * @return
         */
        public Node cloneAndAdopt(Node node) {
            if (node.getOwnerDocument() != get())
                return get().adoptNode(node.cloneNode(true));
            return node;
        }
    }

}
