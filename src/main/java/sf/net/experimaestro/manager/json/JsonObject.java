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

package sf.net.experimaestro.manager.json;

import org.apache.commons.vfs2.FileSystemException;
import org.json.simple.JSONValue;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.ValueType;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.Output;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A JSON object (associates a key to a json value)
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 1/4/13
 */
public class JsonObject
        extends TreeMap<String, Json> /* Warning: we depend on the map being sorted (for hash string) */
        implements Json {
    public JsonObject() {
    }

    public JsonObject(Map<? extends String, ? extends Json> m) {
        super(m);
    }

    @Override
    public String toString() {
        return String.format("{%s}", Output.toString(", ", this.entrySet(),
                entry -> String.format("%s: %s", JSONValue.toJSONString(entry.getKey()), entry.getValue())));
    }

    @Override
    public Json clone() {
        return new JsonObject(this);
    }

    @Override
    public boolean isSimple() {
        if (!containsKey(Manager.XP_VALUE.toString())) {
            return false;
        }

        return ValueType.ATOMIC_TYPES.contains(type());
    }

    @Override
    public Object get() {
        QName parsedType = type();

        Json value = this.get(Manager.XP_VALUE.toString());
        if (value == null)
            throw new IllegalArgumentException("No value in the Json object");

        switch (parsedType.getNamespaceURI()) {

            case Manager.EXPERIMAESTRO_NS:
                switch (parsedType.getLocalPart()) {
                    case "string":
                        if (!(value instanceof JsonString))
                            throw new AssertionError("json value is not a string but" + value.getClass());
                        return value.get();

                    case "real":
                        if (!(value instanceof JsonReal))
                            throw new AssertionError("json value is not a real number but" + value.getClass());
                        return value.get();

                    case "integer":
                        if (!(value instanceof JsonInteger))
                            throw new AssertionError("json value is not an integer but " + value.getClass());
                        return value.get();

                    case "boolean":
                        if (!(value instanceof JsonBoolean))
                            throw new AssertionError("json value is not a boolean but" + value.getClass());
                        return value.get();

                    // TODO: do those checks
                    case "directory":
                    case "file":
                        try {
                            return Scheduler.getVFSManager().resolveFile(value.get().toString());
                        } catch (FileSystemException e) {
                            throw new ExperimaestroRuntimeException(e);
                        }
                    default:
                        throw new ExperimaestroRuntimeException("Un-handled type [%s]", parsedType);
                }

            default:
                throw new ExperimaestroRuntimeException("Un-handled type [%s]", parsedType);
        }
    }

    public void put(String key, String string) {
        put(key, new JsonString(string));
    }

    @Override
    public QName type() {
        Json type = get(Manager.XP_TYPE.toString());
        if (type == null)
            return Manager.XP_OBJECT;

        if (!(type instanceof JsonString))
            throw new IllegalArgumentException("No type in the Json object");

        return QName.parse(type.toString());
    }

    @Override
    public void write(Writer out) throws IOException {
        out.write('{');
        boolean first = true;
        for (Map.Entry<String, Json> entry : this.entrySet()) {
            if (first)
                first = false;
            else
                out.write(", ");

            out.write(JSONValue.toJSONString(entry.getKey()));
            out.write(":");
            entry.getValue().write(out);
        }
        out.write('}');
    }

    @Override
    public boolean canIgnore(Set<QName> ignore) {
        if (ignore.contains(type())) {
            return true;
        }

        if (this.containsKey(Manager.XP_IGNORE.toString())) {
            Json value = this.get(Manager.XP_IGNORE.toString());
            if (value instanceof JsonBoolean) {
                if (((JsonBoolean) value).getBoolean()) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void writeDescriptorString(Writer out, Set<QName> ignore) throws IOException {
        if (canIgnore(ignore)) {
            out.write("null");
            return;
        }

        if (isSimple()) {
            get(Manager.XP_VALUE.toString()).writeDescriptorString(out, ignore);
            return;
        }

        Set<String> ignored_keys = new HashSet<>();
        ignored_keys.add(Manager.XP_IGNORE.toString());
        if (this.containsKey(Manager.XP_IGNORE.toString())) {
            Json value = this.get(Manager.XP_IGNORE.toString());
            if (value instanceof JsonString) {
                ignored_keys.add(((JsonString) value).string);
            } else if (value instanceof JsonArray) {
                for (Json s : (JsonArray) value) {
                    ignored_keys.add(s.toString());
                }
            } else {
                throw new ExperimaestroRuntimeException("Cannot handle xp_ignore of type %s", value.getClass());
            }

        }

        out.write('{');
        boolean first = true;
        for (Map.Entry<String, Json> entry : this.entrySet()) {
            Json value = entry.getValue();
            String key = entry.getKey();
            if (value == null || value.canIgnore(ignore) || key.startsWith("$") || ignored_keys.contains(key))
                continue;

            if (first)
                first = false;
            else
                out.write(",");

            out.write(JSONValue.toJSONString(key));
            out.write(":");
            value.writeDescriptorString(out, ignore);
        }
        out.write('}');
    }

}
