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
import net.bpiwowar.xpm.manager.Constants;
import net.bpiwowar.xpm.manager.TypeName;
import net.bpiwowar.xpm.manager.scripting.Exposed;

import java.io.IOException;

/**
 * A Json integer
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Exposed
public class JsonInteger extends JsonSimple {
    private long value;

    public JsonInteger() {
    }

    public JsonInteger(long value) {
        this.value = value;
    }

    @Override
    public Object get() {
        return value;
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }

    @Override
    public TypeName type() {
        return Constants.XP_INTEGER;
    }

    @Override
    public void writeDescriptorString(JsonWriter writer, JsonWriterOptions options) throws IOException {
        write(writer);
    }

    @Override
    public void write(JsonWriter out) throws IOException {
        out.value(value);
    }


}
