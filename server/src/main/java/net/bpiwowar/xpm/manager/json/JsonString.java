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
import net.bpiwowar.xpm.manager.scripting.Expose;
import net.bpiwowar.xpm.manager.scripting.Exposed;

import java.io.IOException;
import java.io.Writer;

/**
 * A JSON string
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Exposed
public class JsonString extends JsonSimple {
    String string;

    public JsonString() {
    }

    public JsonString(String string) {
        this.string = string;
    }

    @Override
    public Object get() {
        return string;
    }

    @Override
    @Expose
    public String toString() {
        return string;
    }

    @Override
    public TypeName type() {
        return Constants.XP_STRING;
    }

    @Override
    public boolean canIgnore(JsonWriterOptions options) {
        return false;
    }

    @Override
    public void write(JsonWriter out) throws IOException {
        out.value(string);
    }

    @Override
    public void writeDescriptorString(JsonWriter writer, JsonWriterOptions options) throws IOException {
        write(writer);
    }
}
