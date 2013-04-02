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
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonBoolean;
import sf.net.experimaestro.manager.json.JsonInteger;
import sf.net.experimaestro.manager.json.JsonObject;
import sf.net.experimaestro.manager.json.JsonReal;
import sf.net.experimaestro.manager.json.JsonString;
import sf.net.experimaestro.utils.log.Logger;

/**
 * Represents an atomic value
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 13/9/12
 */
public class ValueType extends Type {
    final static private Logger LOGGER = Logger.getLogger();

    static final public QName XP_STRING = new QName(Manager.EXPERIMAESTRO_NS, "string");
    static final public QName XP_REAL = new QName(Manager.EXPERIMAESTRO_NS, "real");
    static final public QName XP_INTEGER = new QName(Manager.EXPERIMAESTRO_NS, "integer");
    static final public QName XP_BOOLEAN = new QName(Manager.EXPERIMAESTRO_NS, "boolean");

    public static final QName XPM_FILE = new QName(Manager.EXPERIMAESTRO_NS, "file");

    public ValueType(QName type) {
        super(type);
    }

    /**
     * Wraps a value into an XML document
     *
     * @param string The value of the element
     * @param type   XML type of the element
     * @return An XML document representing the value
     */
    static public Json wrapString(String string, QName type) {
        JsonObject json = new JsonObject();
        json.put(Manager.XP_VALUE.toString(), string);
        json.put(Manager.XP_TYPE.toString(), type.toString());
        return json;
    }

    static public Json wrap(Integer value) {
        return new JsonInteger(value);
    }

    static public Json wrap(Long value) {
        return new JsonInteger(value);
    }

    static public Json wrap(Float value) {
        return new JsonReal(value);
    }

    static public Json wrap(Double value) {
        return new JsonReal(value);
    }

    static public Json wrap(Boolean value) {
        return new JsonBoolean(value);
    }

    static public Json wrap(FileObject value) {
        return wrapString(value.toString(), XPM_FILE);
    }

    public static Json wrapObject(Object value) {
        if (value instanceof Integer)
            return wrap((Integer) value);
        if (value instanceof Long)
            return wrap((Long) value);
        if (value instanceof Float)
            return wrap((Float) value);
        if (value instanceof Double)
            return wrap((Double) value);

        if (value instanceof FileObject)
            return wrap((FileObject) value);

        // Otherwise, wrap as a string
        return new JsonString(value.toString());
    }

    public static Json wrap(Object value) {
        return wrapObject(value);
    }


    @Override
    public boolean matches(String namespaceURI, String name) {
        return true;
    }

//
//    @Override
//    public void validate(Json element) {
//        if (element.type()
//        return;
//        // FIXME: implement
////        String x = Manager.unwrapToString(element);
////
////        // Test if the value is OK
////        try {
////
////            switch (type.getNamespaceURI()) {
////                case Manager.XMLSCHEMA_NS:
////                    switch (type.getLocalPart()) {
////                        case "string":
////                            break; // we accepts anything
////                        case "float":
////                            Float.parseFloat(x);
////                            break;
////                        case "integer":
////                            Integer.parseInt(x);
////                            break;
////                        case "boolean":
////                            Boolean.parseBoolean(x);
////                            break;
////                        default:
////                            throw new ExperimaestroRuntimeException("Un-handled type [%s]");
////                    }
////                    break;
////
////                case Manager.EXPERIMAESTRO_NS:
////                    switch (type.getLocalPart()) {
////                        // TODO: do those checks
////                        case "directory":
////                            LOGGER.info("Did not check if [%s] was a directory", x);
////                            break;
////                        case "file":
////                            LOGGER.info("Did not check if [%s] was a file", x);
////                            break;
////                        default:
////                            throw new ExperimaestroRuntimeException("Un-handled type [%s]");
////                    }
////                    break;
////
////                default:
////                    throw new ExperimaestroRuntimeException("Un-handled type [%s]");
////            }
////
////        } catch (NumberFormatException e) {
////            ExperimaestroRuntimeException e2 = new ExperimaestroRuntimeException("Wrong value for type [%s]: %s", type, x);
////            throw e2;
////        }
//
//    }


}
