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
import org.json.simple.JSONValue;
import net.bpiwowar.xpm.manager.Constants;
import net.bpiwowar.xpm.manager.QName;
import net.bpiwowar.xpm.manager.scripting.Exposed;

import java.io.IOException;
import java.io.Writer;

/**
 * An immutable real value
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Exposed
public class JsonReal extends Json {
    /** The wrapped value */
    private double value;

    public JsonReal(double value) {
        this.value = value;
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public Object get() {
        return value;
    }

    @Override
    public String toString() {
        return Double.toString(value);
    }

    @Override
    public void write(Writer out) throws IOException {
        JSONValue.writeJSONString(value, out);
    }

    @Override
    public void write(JsonWriter out) throws IOException {
        out.value(value);
    }

    @Override
    public QName type() {
        return Constants.XP_REAL;
    }

    @Override
    public boolean canIgnore(JsonWriterOptions options) {
        return false;
    }

    @Override
    public void writeDescriptorString(Writer writer, JsonWriterOptions options) throws IOException {
        write(writer);
    }

}
