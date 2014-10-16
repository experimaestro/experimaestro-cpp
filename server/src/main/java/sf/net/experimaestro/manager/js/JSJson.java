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

package sf.net.experimaestro.manager.js;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.vfs2.FileObject;
import org.mozilla.javascript.*;
import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.connectors.LocalhostConnector;
import sf.net.experimaestro.connectors.SingleHostConnector;
import sf.net.experimaestro.exceptions.XPMRhinoException;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonArray;
import sf.net.experimaestro.manager.json.JsonObject;
import sf.net.experimaestro.manager.json.JsonWriterOptions;
import sf.net.experimaestro.scheduler.Command;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.RangeUtils;

import java.io.*;

import static com.google.common.collect.Range.closed;

/**
 * Wraps the Json objects
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 1/4/13
 */
public class JSJson extends JSBaseObject implements JSConstructable, Wrapper {
    private final Json json;

    @JSFunction(scope = true)
    public JSJson(Context sc, Scriptable scope, NativeObject object) {
        this.json = JSUtils.toJSON(scope, object);
    }

    @JSFunction(scope = true)
    public JSJson(Context sc, Scriptable scope, NativeArray object) {
        this.json = JSUtils.toJSON(scope, object);
    }

    @JSFunction
    public JSJson(Json json) {
        this.json = json;
        if (json instanceof JSBaseObject)
            setPrototype((Scriptable) json);
    }

    public Json getJson() {
        return json;
    }

    @JSFunction
    public String join(String separator) {
        if (!(json instanceof JsonArray)) {
            throw new IllegalArgumentException("Can only join JSON arrays");
        }
        return Joiner.on(separator).join((JsonArray) json);
    }

    @Override
    @JSFunction
    public String toString() {
        return json.toString();
    }

    @JSFunction
    public String toSource() {
        StringWriter writer = new StringWriter();
        try {
            json.write(writer);
        } catch (IOException e) {
            throw new AssertionError("Should not happen: I/O error while serializing to a StringWriter");
        }
        return writer.toString();
    }


    @Override
    public Object getDefaultValue(Class<?> hint) {
        if (hint == String.class)
            return toString();

    	// We don't return the value since otherwise hidden bug could be introduced in scripts:
    	// scripts should always access json value through calling it
        return "Json";
    }


    @Override
    public Object get(String name, Scriptable start) {
        if (json instanceof JsonArray) {
            switch(name) {
                case "length":
                    return ((JsonArray) json).size();
            }
        }

        if (json instanceof JsonObject) {
            Json value = ((JsonObject) json).get(name);
            if (value != null)
                return new JSJson(value);
        }

        return super.get(name, start);
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        if (json instanceof JsonObject) {
            ((JsonObject) json).put(name, JSUtils.toJSON(start, value));
            return;
        }
        super.put(name, start, value);
    }

    @Override
    public Object get(int index, Scriptable start) {
        if (json instanceof JsonArray)
            return new JSJson(((JsonArray) json).get(index));

        if (json instanceof JsonObject) {
            return get(Integer.toString(index), start);
        }
        return super.get(index, start);
    }

    @Override
    public Object[] getIds() {
        if (json instanceof JsonArray)
            return RangeUtils.toIntegerArray(closed(0, ((JsonArray) json).size()));

        if (json instanceof JsonObject)
            return ((JsonObject) json).keySet().toArray();

        return super.getIds();
    }

    @Override
    public boolean has(String name, Scriptable start) {
        if (json instanceof JsonObject) {
            final JsonObject jsonObject = (JsonObject) json;
            return jsonObject.containsKey(name);
        }
        return super.has(name, start);
    }

    @Override
    public boolean has(int index, Scriptable start) {
        if (json instanceof JsonArray)
            return index >= 0 && index < ((JsonArray) json).size();

        return super.has(index, start);
    }

    @JSFunction
    public String get_descriptor() throws IOException {
        StringWriter writer = new StringWriter();
        json.writeDescriptorString(writer);
        return writer.toString();
    }

    @Override
    public Object unwrap() {
        return json;
    }

    @JSFunction(value = "as_parameter_file", optional = 1)
    @JSHelp("Creates a parameter file from this JSON")
    public Command.ParameterFile asParameterFile(String id, SingleHostConnector connector) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        if (connector == null) {
            connector = xpm.getConnector().getMainConnector();
        }
        Writer writer = new OutputStreamWriter(bytes);
        final SingleHostConnector finalConnector = connector;
        JsonWriterOptions options = new JsonWriterOptions(ImmutableSet.of())
                .simplifyValues(true)
                .ignore$(false)
                .ignoreNull(false)
                .resolveFile(f -> {
                    try { return finalConnector.resolve(f); }
                    catch(Exception e) {
                        throw new XPMRhinoException(e);
                    }
                });
        json.writeDescriptorString(writer, options);
        writer.flush();
        return new Command.ParameterFile(id, bytes.toByteArray());
    }
}
