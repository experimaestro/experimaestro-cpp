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

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonWriter;
import net.bpiwowar.xpm.commands.ParameterFile;
import net.bpiwowar.xpm.connectors.SingleHostConnector;
import net.bpiwowar.xpm.exceptions.WrappedException;
import net.bpiwowar.xpm.exceptions.XPMRhinoException;
import net.bpiwowar.xpm.exceptions.XPMScriptRuntimeException;
import net.bpiwowar.xpm.manager.Constants;
import net.bpiwowar.xpm.manager.TypeName;
import net.bpiwowar.xpm.manager.scripting.Expose;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.manager.scripting.Help;
import net.bpiwowar.xpm.manager.scripting.LanguageContext;
import net.bpiwowar.xpm.manager.scripting.ScriptContext;
import net.bpiwowar.xpm.manager.scripting.ScriptingPath;
import net.bpiwowar.xpm.scheduler.Resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Base class for all JSON objects
 * <p>
 * Objects can be sealed to avoid unnecessary copies
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Exposed
abstract public class Json {
    public Json() {
    }

    /**
     * Returns true if this Json object is a simple type
     */
    public boolean isSimple() {
        return true;
    }

    /**
     * Returns the simple value underlying this object
     *
     * @return
     */
    abstract public Object get();

    /**
     * Get the XPM type
     */
    public abstract TypeName type();

    public boolean canIgnore(JsonWriterOptions options) {
        return false;
    }

    /**
     * Write a normalized version of the JSON
     */
    public void writeDescriptorString(JsonWriter writer, JsonWriterOptions options) throws IOException {
        write(writer);
    }

    /**
     * Write a normalized version of the JSON
     */
    final public void writeDescriptorString(Writer writer, JsonWriterOptions options) throws IOException {
        writeDescriptorString(new JsonWriter(writer), options);
    }

    /**
     * Write a normalized version of the JSON
     *
     * @param writer
     */
    public void writeDescriptorString(JsonWriter writer) throws IOException {
        writeDescriptorString(writer, JsonWriterOptions.DEFAULT_OPTIONS);
    }

    public void writeDescriptorString(Writer writer) throws IOException {
        final JsonWriter jsonWriter = new JsonWriter(writer);
        jsonWriter.setLenient(true);
        writeDescriptorString(jsonWriter);
    }

    /**
     * Write with a structured writer
     *
     * @param out The writer
     */
    abstract public void write(JsonWriter out) throws IOException;

    @Expose(context = true)
    static public Json of(LanguageContext lcx, ScriptContext cx, Object object) {
        return lcx.toJSON(object);
    }

    @Expose
    public String toSource() {
        try (StringWriter writer = new StringWriter();
             final JsonWriter jsonWriter = new JsonWriter(writer)) {
            try {
                write(jsonWriter);
            } catch (IOException e) {
                throw new AssertionError("Should not happen: I/O error while serializing to a StringWriter");
            }
            return writer.toString();
        } catch (IOException e) {
            throw new WrappedException(e);
        }
    }

    @Expose
    public String get_descriptor() throws IOException {
        try (StringWriter writer = new StringWriter();
             final JsonWriter jsonWriter = new JsonWriter(writer)) {
            writeDescriptorString(jsonWriter);
            return writer.toString();
        } catch (IOException e) {
            throw new WrappedException(e);
        }
    }

    @Expose
    final public boolean is_array() {
        return this instanceof JsonArray;
    }

    @Expose
    final public boolean is_object() {
        return this instanceof JsonObject;
    }

    @Expose("as_object")
    public JsonObject asObject() {
        if (isSimple()) {
            final JsonObject jsonObject = new JsonObject();
            jsonObject.put(Constants.XP_VALUE.toString(), this);
            jsonObject.put(Constants.XP_TYPE.toString(), type().toString());
            return jsonObject;
        }
        throw new XPMScriptRuntimeException("Cannot transform into a JSON object");
    }

    @Expose(value = "as_parameter_file", optional = 1)
    @Help("Creates a parameter file from this JSON")
    public ParameterFile asParameterFile(String id, SingleHostConnector connector) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        if (connector == null) {
            connector = ScriptContext.get().getConnector().getMainConnector();
        }
        Writer writer = new OutputStreamWriter(bytes);
        final SingleHostConnector finalConnector = connector;
        JsonWriterOptions options = JsonWriterOptions.PARAMETER_OPTIONS.clone()
                .resolveFile(f -> {
                    try {
                        return finalConnector.resolve(f);
                    } catch (Exception e) {
                        throw new XPMRhinoException(e);
                    }
                });
        writeDescriptorString(writer, options);
        writer.flush();
        return new ParameterFile(id, bytes.toByteArray());
    }

    /**
     * Perform a copy of the JSON object so that it can be modified.
     * <p>
     * This method has to be overwritten by classes that are not immutable,
     * since by default it returns the object itself.
     *
     * @return A new JSON object
     */
    public Json copy() {
        return this;
    }

    /**
     * Seal the object
     * <p>
     * This method has to be overwritten by classes that are not immutable
     * since this method does nothing by default.
     */
    public Json seal() {
        return this;
    }

    /**
     * Convert an object to a  JSON
     *
     * @param lcx
     * @param value
     * @return
     */
    public static Json toJSON(LanguageContext lcx, Object value) {
        if (value instanceof net.bpiwowar.xpm.manager.scripting.Wrapper) {
            value = ((net.bpiwowar.xpm.manager.scripting.Wrapper) value).unwrap();
        }

        if (value instanceof JsonElement) {
            return toJSON((JsonElement) value);
        }

        if (value instanceof Json)
            return (Json) value;

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
            return JsonBoolean.of((Boolean) value);

        // -- An array
        if (value.getClass().isArray()) {
            final int length = Array.getLength(value);
            JsonArray json = new JsonArray();
            for (int i = 0; i < length; i++)
                json.add(toJSON(lcx, Array.get(value, i)));
            return json;
        }

        if (value instanceof List) {
            final List list = (List) value;
            JsonArray json = new JsonArray();
            for (Object element : list)
                json.add(toJSON(lcx, element));
            return json;
        }

        // Maps
        if (value instanceof Map) {
            return JsonObject.toJSON(lcx, (Map) value);
        }

        if (value instanceof java.nio.file.Path)
            return new JsonPath((java.nio.file.Path) value);

        if (value instanceof Path)
            return new JsonPath(((ScriptingPath) value).getObject());

        if (value instanceof Resource)
            return new JsonResource((Resource) value);

        if (value instanceof BigInteger) {
            return new JsonInteger(((BigInteger) value).longValueExact());
        }

        return new JsonString(value.toString());

    }

    public static Json toJSON(JsonElement element) {
        if (element.isJsonNull()) {
            return JsonNull.getSingleton();
        }

        if (element.isJsonPrimitive()) {
            final JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) return JsonBoolean.of(primitive.getAsBoolean());
            if (primitive.isNumber()) {
                final String stringNumber = primitive.getAsString();
                try {
                    return new JsonInteger(Long.parseLong(stringNumber));
                } catch (NumberFormatException nfe) {
                    return new JsonReal(Double.parseDouble(stringNumber));
                }

            }
            if (primitive.isString()) return new JsonString(primitive.getAsString());
            throw new AssertionError("Unknown JSON primitive type " + primitive);
        }

        if (element.isJsonArray()) {
            final JsonArray array = new JsonArray();
            element.getAsJsonArray().forEach(m -> array.add(toJSON(null, m)));
            return array;
        }

        if (element.isJsonObject()) {
            final JsonObject object = new JsonObject();
            element.getAsJsonObject().entrySet().stream().forEach(e -> object.put(e.getKey(), toJSON(e.getValue())));
            return object;
        }
        throw new AssertionError("Unknown JSON type " + element);
    }

    public void write(Writer writer) throws IOException {
        write(new JsonWriter(writer));
    }

    public abstract Json annotate(String key, Json value);
}
