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

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.XMLUtils;
import sf.net.experimaestro.utils.log.Logger;

/**
 * Represents an atomic value
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 13/9/12
 */
public class ValueType extends Type {
    final static private Logger LOGGER = Logger.getLogger();

    static final public QName TYPE = new QName(Manager.EXPERIMAESTRO_NS, "type");
    static final public QName VALUE = new QName(Manager.EXPERIMAESTRO_NS, "value");

    static final public QName XS_STRING = new QName(Manager.XMLSCHEMA_NS, "string");
    static final public QName XS_FLOAT = new QName(Manager.XMLSCHEMA_NS, "float");
    static final public QName XS_INTEGER = new QName(Manager.XMLSCHEMA_NS, "integer");

    static final public QName XS_BOOLEAN = new QName(Manager.XMLSCHEMA_NS, "boolean");

    public static final QName XPM_FILE = new QName(Manager.EXPERIMAESTRO_NS, "file");

    private QName type;

    public ValueType(QName type) {
        super(null);
        this.type = type;
    }

    /**
     * Wraps a value into an XML document
     *
     * @param namespace The namespace URI
     * @param name      The local name of the element
     * @param string    The value of the element
     * @param type      XML type of the element
     * @return An XML document representing the value
     */
    static public Document wrapString(String namespace, String name, String string, QName type) {
        final Document doc = XMLUtils.newDocument();
        Element element = doc.createElementNS(namespace, name);
        element.setAttributeNS(Manager.EXPERIMAESTRO_NS, "value", string);
        if (type != null)
            element.setAttributeNS(Manager.EXPERIMAESTRO_NS, "type", type.toString());
        doc.appendChild(element);
        return doc;
    }

    static public Document wrap(String namespace, String name, Integer value) {
        return wrapString(namespace, name, Integer.toString(value), XS_INTEGER);
    }

    static public Document wrap(String namespace, String name, Long value) {
        return wrapString(namespace, name, Long.toString(value), XS_INTEGER);
    }

    static public Document wrap(String namespace, String name, Float value) {
        return wrapString(namespace, name, Float.toString(value), XS_INTEGER);
    }

    static public Document wrap(String namespace, String name, Double value) {
        return wrapString(namespace, name, Double.toString(value), XS_FLOAT);
    }

    static public Document wrap(String namespace, String name, Boolean value) {
        return wrapString(namespace, name, Boolean.toString(value), XS_BOOLEAN);
    }

    static public Document wrap(String namespace, String name, FileObject value) {
        return wrapString(namespace, name, value.toString(), XPM_FILE);
    }

    public static Document wrapObject(String namespace, String name, Object value) {
        if (value instanceof Integer)
            return wrap(namespace, name, (Integer) value);
        if (value instanceof Long)
            return wrap(namespace, name, (Long) value);
        if (value instanceof Float)
            return wrap(namespace, name, (Float) value);
        if (value instanceof Double)
            return wrap(namespace, name, (Double) value);

        if (value instanceof FileObject)
            return wrap(namespace, name, (FileObject) value);

        return wrapString(namespace, name, value.toString(), null);
    }

    public static Document wrap(Object value) {
        return wrapObject(Manager.EXPERIMAESTRO_NS, "value", value);
    }

    public QName getValueType() {
        return type;
    }

    @Override
    public String toString() {
        return super.toString() + ": " + type.toString();
    }

    @Override
    public boolean matches(String namespaceURI, String name) {
        return true;
    }



    @Override
    public void validate(Element element) {
        String x = Manager.unwrapToString(element);

        // Test if the value is OK
        try {

            switch (type.getNamespaceURI()) {
                case Manager.XMLSCHEMA_NS:
                    switch (type.getLocalPart()) {
                        case "string":
                            break; // we accepts anything
                        case "float":
                            Float.parseFloat(x);
                            break;
                        case "integer":
                            Integer.parseInt(x);
                            break;
                        default:
                            throw new ExperimaestroRuntimeException("Un-handled type [%s]");
                    }
                    break;

                case Manager.EXPERIMAESTRO_NS:
                    switch (type.getLocalPart()) {
                        // TODO: do those checks
                        case "directory":
                            LOGGER.info("Did not check if [%s] was a directory", x);
                            break;
                        case "file":
                            LOGGER.info("Did not check if [%s] was a file", x);
                            break;
                        default:
                            throw new ExperimaestroRuntimeException("Un-handled type [%s]");
                    }
                    break;

                default:
                    throw new ExperimaestroRuntimeException("Un-handled type [%s]");
            }

        } catch (NumberFormatException e) {
            ExperimaestroRuntimeException e2 = new ExperimaestroRuntimeException("Wrong value for type [%s]: %s", type, x);
            throw e2;
        }

        // Annotate the XML
        element.setAttributeNS(TYPE.getNamespaceURI(), TYPE.getLocalPart(), type.toString());
    }

    static public Object unwrap(Element element) {
        String x = element.getAttributeNS(VALUE.getNamespaceURI(), VALUE.getLocalPart());

        if (!TYPE.isAttribute(element))
            return x;

        QName type = QName.parse(TYPE.getAttribute(element));

        switch (type.getNamespaceURI()) {
            case Manager.XMLSCHEMA_NS:
                switch (type.getLocalPart()) {
                    case "string":
                        return x;
                    case "float":
                        return Float.parseFloat(x);
                    case "integer":
                        return Integer.parseInt(x);
                    default:
                        throw new ExperimaestroRuntimeException("Un-handled type [%s]");
                }

            case Manager.EXPERIMAESTRO_NS:
                switch (type.getLocalPart()) {
                    // TODO: do those checks
                    case "directory":
                    case "file":
                        try {
                            return Scheduler.getVFSManager().resolveFile(x);
                        } catch (FileSystemException e) {
                            throw new ExperimaestroRuntimeException(e);
                        }
                    default:
                        throw new ExperimaestroRuntimeException("Un-handled type [%s]");
                }

            default:
                throw new ExperimaestroRuntimeException("Un-handled type [%s]");
        }
    }

}
