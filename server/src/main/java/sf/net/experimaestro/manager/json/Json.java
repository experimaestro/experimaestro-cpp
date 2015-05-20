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

import com.google.common.collect.ImmutableSet;
import com.google.gson.stream.JsonWriter;
import sf.net.experimaestro.connectors.SingleHostConnector;
import sf.net.experimaestro.exceptions.XPMRhinoException;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.scripting.*;
import sf.net.experimaestro.scheduler.Command;

import java.io.*;

/**
 * Base class for all JSON objects
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
    boolean isSimple() {
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
    public abstract QName type();

    public boolean canIgnore(JsonWriterOptions options) {
        return false;
    }

    /**
     * Write a normalized version of the JSON
     */
    public void writeDescriptorString(Writer writer, JsonWriterOptions options) throws IOException {
        write(writer);
    }

    /**
     * Write a normalized version of the JSON
     */
    public void writeDescriptorString(Writer writer) throws IOException {
        writeDescriptorString(writer, JsonWriterOptions.DEFAULT_OPTIONS);
    }

    /**
     * Write a JSON representation
     *
     * @param out
     * @throws IOException
     */
    abstract public void write(Writer out) throws IOException;

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
        StringWriter writer = new StringWriter();
        try {
            write(writer);
        } catch (IOException e) {
            throw new AssertionError("Should not happen: I/O error while serializing to a StringWriter");
        }
        return writer.toString();
    }

    @Expose
    public String get_descriptor() throws IOException {
        StringWriter writer = new StringWriter();
        writeDescriptorString(writer);
        return writer.toString();
    }

    @Expose
    final public boolean is_array() {
        return this instanceof JsonArray;
    }

    @Expose
    final public boolean is_object() {
        return this instanceof JsonObject;
    }


    @Expose(value = "as_parameter_file", optional = 1)
    @Help("Creates a parameter file from this JSON")
    public Command.ParameterFile asParameterFile(String id, SingleHostConnector connector) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        if (connector == null) {
            connector = ScriptContext.get().getConnector().getMainConnector();
        }
        Writer writer = new OutputStreamWriter(bytes);
        final SingleHostConnector finalConnector = connector;
        JsonWriterOptions options = new JsonWriterOptions(ImmutableSet.of())
                .simplifyValues(true)
                .ignore$(false)
                .ignoreNull(false)
                .resolveFile(f -> {
                    try {
                        return finalConnector.resolve(f);
                    } catch (Exception e) {
                        throw new XPMRhinoException(e);
                    }
                });
        writeDescriptorString(writer, options);
        writer.flush();
        return new Command.ParameterFile(id, bytes.toByteArray());
    }

    @Override
    abstract public Json clone();
}
