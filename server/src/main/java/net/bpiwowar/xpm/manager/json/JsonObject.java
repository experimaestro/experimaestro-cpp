package net.bpiwowar.xpm.manager.json;

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
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.manager.Constants;
import net.bpiwowar.xpm.manager.TypeName;
import net.bpiwowar.xpm.manager.scripting.Expose;
import net.bpiwowar.xpm.manager.scripting.ExposeMode;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.manager.scripting.LanguageContext;
import net.bpiwowar.xpm.utils.Output;
import net.bpiwowar.xpm.utils.PathUtils;

import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;

import static java.lang.String.format;

/**
 * A JSON object (associates a key to a json value)
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Exposed
public class JsonObject extends Json {
    /**
     * True if the object is sealed
     */
    private boolean sealed = false;

    private TreeMap<String, Json> map = new TreeMap<>(); /* Warning: we depend on the map being sorted (for hash string) */

    public static final String XP_TYPE_STRING = Constants.XP_TYPE.toString();
    public static final String XP_VALUE_STRING = Constants.XP_VALUE.toString();

    public JsonObject() {
    }

    public JsonObject(TreeMap<String, Json> map) {
        this.map.putAll(map);
    }


    @Override
    public String toString() {
        return format("{%s}", Output.toString(", ", map.entrySet(),
                entry -> format("%s: %s", entry.getKey(), entry.getValue())));
    }

    @Override
    public boolean isSimple() {
        if (!map.containsKey(Constants.XP_VALUE.toString())) {
            return false;
        }

        return Constants.ATOMIC_TYPES.contains(type());
    }


    /** Get the value json */
    Json valueAsJson() {
        TypeName parsedType = type();

        Json value = map.get(Constants.XP_VALUE.toString());
        if (value == null)
            throw new IllegalArgumentException("No value in the Json object");

        if (parsedType.getNamespaceURI().equals("")) {
            switch (parsedType.getLocalPart()) {
                case "string":
                    if (!(value instanceof JsonString))
                        throw new AssertionError("json value is not a string but" + value.getClass());
                    return value;

                case "real":
                    if (!(value instanceof JsonReal))
                        throw new AssertionError("json value is not a real number but" + value.getClass());
                    return value;

                case "integer":
                    if (!(value instanceof JsonInteger))
                        throw new AssertionError("json value is not an integer but " + value.getClass());
                    return value;

                case "boolean":
                    if (!(value instanceof JsonBoolean))
                        throw new AssertionError("json value is not a boolean but" + value.getClass());
                    return value;

                // TODO: do checks
                case "directory":
                case "file":
                case "path":
                    final String uri = value.get().toString();
                    try {
                        return new JsonPath(PathUtils.toPath(uri));
                    } catch (IOException e) {
                        throw new XPMRuntimeException("Could not convert path %s", uri);
                    }
                default:
                    throw new XPMRuntimeException("Un-handled type [%s]", parsedType);
            }
        }

        throw new XPMRuntimeException("Un-handled type [%s]", parsedType);
    }

    @Override
    public Object get() {
        return valueAsJson().get();
    }

    public void put(String key, String string) {
        if (sealed) {
            throw new UnsupportedOperationException("Cannot add entries to a sealed JSON object");
        }
        if (string == null) {
            map.put(key, JsonNull.getSingleton());
        } else {
            map.put(key, new JsonString(string));
        }
    }

    @Expose
    public Iterable items() {
        return map.entrySet();
    }

    @Expose(mode = ExposeMode.FIELDS)
    public void put(String key, Json json) {
        if (sealed) {
            throw new UnsupportedOperationException("Cannot add entries to a sealed JSON object");
        }
        if (json == null) {
            map.put(key, JsonNull.getSingleton());
        } else {
            map.put(key, json);
        }
    }

    @Override
    public TypeName type() {
        Json type = map.get(Constants.XP_TYPE.toString());
        if (type == null)
            return Constants.XP_OBJECT;

        if (!(type instanceof JsonString))
            throw new IllegalArgumentException("No type in the Json object");

        return TypeName.parse(type.toString());
    }


    @Override
    public void write(JsonWriter out) throws IOException {
        out.beginObject();
        for (Map.Entry<String, Json> entry : map.entrySet()) {
            out.name(entry.getKey());
            entry.getValue().write(out);
        }
        out.endObject();
    }

    @Override
    public JsonObject asObject() {
        return this;
    }

    @Override
    public Json copy() {
        final JsonObject copy = new JsonObject();
        this.map.forEach((x, y) -> copy.put(x, y.copy()));
        return copy;
    }

    @Override
    public Json seal() {
        if (!sealed) {
            sealed = true;
            this.map.values().forEach(x -> x.seal());
        }
        return this;
    }

    @Override
    public Json annotate(String key, Json value) {
        put(key, value);
        return this;
    }

    @Override
    public boolean canIgnore(JsonWriterOptions options) {
        if (options.ignore.contains(type())) {
            return true;
        }

        if (options.removeDefault) {
            final Json value = map.getOrDefault(Constants.JSON_KEY_DEFAULT, null);
            if (value != null && value.isSimple()) {
                final Object o = value.get();
                if (o instanceof Boolean) return ((Boolean) o);
            }
        }

        if (map.containsKey(Constants.XP_IGNORE.toString())) {
            Json value = map.get(Constants.XP_IGNORE.toString());
            if (value instanceof JsonBoolean) {
                if (((JsonBoolean) value).getBoolean()) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void writeDescriptorString(JsonWriter out, JsonWriterOptions options) throws IOException {
        if (canIgnore(options)) {
            out.nullValue();
            return;
        }

        if (isSimple() && options.simplifyValues) {
            this.valueAsJson().writeDescriptorString(out, options);
            return;
        }

        Set<String> ignored_keys = new HashSet<>();
        ignored_keys.add(Constants.XP_IGNORE.toString());
        if (map.containsKey(Constants.XP_IGNORE.toString())) {
            Json value = map.get(Constants.XP_IGNORE.toString());
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

        out.beginObject();
        for (Map.Entry<String, Json> entry : map.entrySet()) {
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

            out.name(key);
            value.writeDescriptorString(out, options);
        }
        out.endObject();
    }

    @Expose(mode = ExposeMode.FIELDS)
    public Json getField(String name) {
        return JsonObject.this.get(name);
    }

    public Json get(String name) {
        return map.get(name);
    }

    public Iterable<? extends Json> values() {
        return map.values();
    }

    public boolean containsKey(String key) {
        return map.containsKey(key);
    }

    public Iterable<? extends Map.Entry<String, Json>> entrySet() {
        return map.entrySet();
    }

    public static JsonObject toJSON(LanguageContext lcx, Map<?, ?> map) {
        JsonObject json = new JsonObject();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            TypeName qname = lcx.qname(entry.getKey());
            Object pValue = entry.getValue();

            if (qname.equals(Constants.XP_TYPE)) {
                pValue = lcx.qname(entry.getValue());
            }

            String key = qname.toString();
            final Json key_value = Json.toJSON(lcx, pValue);
            json.put(key, key_value);
        }
        return json;
    }

    public void forEach(BiConsumer<? super String, ? super Json> action) {
        Objects.requireNonNull(action);
        for (Map.Entry<? super String, ? super Json> entry : entrySet()) {
            action.accept((String) entry.getKey(), (Json) entry.getValue());
        }
    }

}
