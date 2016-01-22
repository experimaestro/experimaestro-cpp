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
    public static HashSet<TypeName> ATOMIC_TYPES = new HashSet<>();

    public static final String EXPERIMAESTRO_NS = "xpm";
    /** Corresponds to a directory on disk */
    public static final TypeName XP_DIRECTORY = registerAtomicType(null, "directory");
    /** Corresponds to a file on disk */
    public static final TypeName XP_FILE = registerAtomicType(null, "file");
    /** Type path is generic (file or directory) */
    public static final TypeName XP_PATH = registerAtomicType(null, "path");
    static final public TypeName XP_RESOURCE_TYPE = registerAtomicType(null, "resource");
    static final public TypeName XP_BOOLEAN = registerAtomicType(null, "boolean");
    static final public TypeName XP_INTEGER = registerAtomicType(null, "integer");
    static final public TypeName XP_REAL = registerAtomicType(null, "real");
    static final public TypeName XP_STRING = registerAtomicType(null, "string");
    public static final TypeName XP_ANY = new TypeName(null, "any");
    public static final TypeName XP_OBJECT = new TypeName(null, "object");
    public static final TypeName XP_INPUT_STREAM = new TypeName(null, "input-stream");
    public static final TypeName XP_ARRAY = new TypeName(null, "array");
    public static final Namespace EXPERIMAESTRO_NS_OBJECT = new Namespace(EXPERIMAESTRO_NS, "xp");
    public static final Map<String, String> PREDEFINED_PREFIXES = new TreeMap<>();
    public static final String EXPERIMAESTRO_PREFIX = "xp";
    public static final TypeName XP_TYPE = new TypeName(null, "$type");
    /**
     * The simple value of the object
     */
    public static final TypeName XP_VALUE = new TypeName(null, "$value");
    /**
     * The resource associated with the object
     */
    public static final TypeName XP_RESOURCE = new TypeName(null, "$resource");
    /// Ignored value
    public static final TypeName XP_IGNORE = new TypeName(null, "$ignore");
    public static final String JSON_TAG_NAME = "$tag";

    public static final String XPM_SIGNATURE = "signature.xpm";
    public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    public static final String JAVATASK_INTROSPECTION_PATH = "META-INF/net.bpiwowar.experimaestro/introspection.json";
    public static final String JAVATASK_TASKS_PATH = "META-INF/net.bpiwowar.experimaestro/tasks.json";

    static {
        PREDEFINED_PREFIXES.put("xp", EXPERIMAESTRO_NS);
    }

    private static TypeName registerAtomicType(String ns, String local) {
        TypeName typeName = new TypeName(ns, local);
        ATOMIC_TYPES.add(typeName);
        return typeName;
    }
}
