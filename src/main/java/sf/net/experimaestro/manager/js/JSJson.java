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
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonArray;
import sf.net.experimaestro.manager.json.JsonObject;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.RangeUtils;

import java.io.IOException;
import java.io.StringWriter;

import static com.google.common.collect.Ranges.closed;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 1/4/13
 */
public class JSJson extends JSBaseObject implements Wrapper {
    private final Json json;

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
            json.toJSONString(writer);
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
        }
        super.put(name, start, value);
    }

    @Override
    public Object get(int index, Scriptable start) {
        if (json instanceof JsonArray)
            return new JSJson(((JsonArray) json).get(index));
        return super.get(index, start);
    }

    @Override
    public Object[] getIds() {
        if (json instanceof JsonArray)
            return RangeUtils.toIntegerArray(closed(0, ((JsonArray) json).size()));

        return super.getIds();
    }

    @Override
    public Object unwrap() {
        return json;
    }
}
