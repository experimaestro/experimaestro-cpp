package sf.net.experimaestro.manager.json;

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

import com.google.gson.stream.JsonWriter;
import org.json.simple.JSONValue;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.ValueType;
import sf.net.experimaestro.utils.Output;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A JSON object (associates a key to a json value)
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class JsonObject
        extends TreeMap<String, Json> /* Warning: we depend on the map being sorted (for hash string) */
        implements Json {

    public static final String XP_TYPE_STRING = Manager.XP_TYPE.toString();
    public static final String XP_VALUE_STRING = Manager.XP_VALUE.toString();

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

                    // TODO: do checks
                    case "directory":
                    case "file":
                    case "path":
                        return Paths.get(value.get().toString());
                    default:
                        throw new XPMRuntimeException("Un-handled type [%s]", parsedType);
                }

            default:
                throw new XPMRuntimeException("Un-handled type [%s]", parsedType);
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
            if (entry.getValue() == null)
                out.write("null");
            else
                entry.getValue().write(out);
        }
        out.write('}');
    }

    @Override
    public void write(JsonWriter out) throws IOException {
        out.beginObject();
        for (Map.Entry<String, Json> entry : this.entrySet()) {
            out.name(entry.getKey());
            entry.getValue().write(out);
        }
        out.endObject();
    }

    @Override
    public boolean canIgnore(JsonWriterOptions options) {
        if (options.ignore.contains(type())) {
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
    public void writeDescriptorString(Writer out, JsonWriterOptions options) throws IOException {
        if (canIgnore(options)) {
            out.write("null");
            return;
        }

        if (isSimple() && options.simplifyValues) {
            get(Manager.XP_VALUE.toString()).writeDescriptorString(out, options);
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
                throw new XPMRuntimeException("Cannot handle $ignore of type %s", value.getClass());
            }

        }

        out.write('{');
        boolean first = true;
        for (Map.Entry<String, Json> entry : this.entrySet()) {
            Json value = entry.getValue();
            String key = entry.getKey();
            // A key is ignored if either:
            // - its value is null or can be ignored
            // - it starts with "$" and is not XP_TYPE or XP_VALUE
            // - it is in the $$ignore key
            if ((value == null && options.ignoreNull)
                    || value.canIgnore(options)
                    || (options.ignore$ && key.startsWith("$") && !key.equals(XP_TYPE_STRING) && !key.equals(XP_VALUE_STRING))
                    || ignored_keys.contains(key))
                continue;

            if (first)
                first = false;
            else
                out.write(",");

            out.write(JSONValue.toJSONString(key));
            out.write(":");
            value.writeDescriptorString(out, options);
        }
        out.write('}');
    }

}
