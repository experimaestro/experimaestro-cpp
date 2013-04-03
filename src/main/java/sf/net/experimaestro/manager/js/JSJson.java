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

import org.mozilla.javascript.Scriptable;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonArray;
import sf.net.experimaestro.manager.json.JsonInteger;
import sf.net.experimaestro.manager.json.JsonObject;
import sf.net.experimaestro.manager.json.JsonReal;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.RangeUtils;

import static com.google.common.collect.Ranges.closed;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 1/4/13
 */
public class JSJson extends JSBaseObject {
    private final Json json;

    public JSJson(Json json) {
        this.json = json;
        if (json instanceof JSBaseObject)
            setPrototype((Scriptable) json);
    }

    public Json getJson() {
        return json;
    }

    @JSFunction(call = true)
    public Object call() {
        return json.get();
    }

    @Override
    public String toString() {
        return json.toString();
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        if (hint == null)
            if (json.isSimple())
                return json.get();

        if (hint == Number.class) {
            if (json instanceof JsonInteger || json instanceof JsonReal)
                return json.get();
            return Double.parseDouble(json.toString());
        }

        return json.toString();
    }


    @Override
    public Object get(String name, Scriptable start) {
        if (json instanceof JsonArray && "length".equals(name))
            return ((JsonArray) json).size();

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

}
