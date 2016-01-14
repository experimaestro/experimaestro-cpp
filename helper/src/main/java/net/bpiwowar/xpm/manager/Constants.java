/*
 *
 *  * This file is part of experimaestro.
 *  * Copyright (c) 2015 B. Piwowarski <benjamin@bpiwowar.net>
 *  *
 *  * experimaestro is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * experimaestro is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package net.bpiwowar.xpm.manager;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

/**
 * Constants (namespaces and others) used by experimaestro
 */
public class Constants {
    public static final String XPM_NOTIFICATION_URL = "XPM_NOTIFICATION_URL";
    // Set of all atomic types
    public static HashSet<QName> ATOMIC_TYPES = new HashSet<>();

    public static final String EXPERIMAESTRO_NS = "http://experimaestro.lip6.fr";
    public static final QName XP_XML = registerAtomicType(EXPERIMAESTRO_NS, "xml");
    /** Corresponds to a directory on disk */
    public static final QName XP_DIRECTORY = registerAtomicType(EXPERIMAESTRO_NS, "directory");
    /** Corresponds to a file on disk */
    public static final QName XP_FILE = registerAtomicType(EXPERIMAESTRO_NS, "file");
    /** Type path is generic (file or directory) */
    public static final QName XP_PATH = registerAtomicType(EXPERIMAESTRO_NS, "path");
    static final public QName XP_RESOURCE_TYPE = registerAtomicType(EXPERIMAESTRO_NS, "resource");
    static final public QName XP_BOOLEAN = registerAtomicType(EXPERIMAESTRO_NS, "boolean");
    static final public QName XP_INTEGER = registerAtomicType(EXPERIMAESTRO_NS, "integer");
    static final public QName XP_REAL = registerAtomicType(EXPERIMAESTRO_NS, "real");
    static final public QName XP_STRING = registerAtomicType(EXPERIMAESTRO_NS, "string");
    public static final QName XP_ANY = new QName(EXPERIMAESTRO_NS, "any");
    public static final QName XP_OBJECT = new QName(EXPERIMAESTRO_NS, "object");
    public static final QName XP_INPUT_STREAM = new QName(EXPERIMAESTRO_NS, "input-stream");
    public static final QName XP_ARRAY = new QName(EXPERIMAESTRO_NS, "array");
    public static final Namespace EXPERIMAESTRO_NS_OBJECT = new Namespace(EXPERIMAESTRO_NS, "xp");
    public static final Map<String, String> PREDEFINED_PREFIXES = new TreeMap<>();
    public static final String EXPERIMAESTRO_PREFIX = "xp";
    public static final QName XP_TYPE = new QName(null, "$type");
    /**
     * The simple value of the object
     */
    public static final QName XP_VALUE = new QName(null, "$value");
    /**
     * The resource associated with the object
     */
    public static final QName XP_RESOURCE = new QName(null, "$resource");
    /// Ignored value
    public static final QName XP_IGNORE = new QName(null, "$ignore");
    public static final String XPM_SIGNATURE = "signature.xpm";
    public static final String OLD_XPM_SIGNATURE = ".xpm-signature";
    public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    public static final String JAVATASK_INTROSPECTION_PATH = "META-INF/net.bpiwowar.experimaestro/introspection.json";
    public static final String JAVATASK_TASKS_PATH = "META-INF/net.bpiwowar.experimaestro/tasks.json";

    static {
        PREDEFINED_PREFIXES.put("xp", EXPERIMAESTRO_NS);
    }

    private static QName registerAtomicType(String ns, String local) {
        QName qName = new QName(ns, local);
        ATOMIC_TYPES.add(qName);
        return qName;
    }
}
