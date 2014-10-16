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

package sf.net.experimaestro.manager.json;

import com.sleepycat.persist.model.Persistent;
import org.json.simple.JSONValue;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.ValueType;

import java.io.IOException;
import java.io.Writer;

/**
 * A JSON string
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Persistent
public class JsonString implements Json {
    String string;

    public JsonString() {}

    public JsonString(String string) {
        this.string = string;
    }

    @Override
    public Json clone() {
        return new JsonString(string);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public Object get() {
        return string;
    }

    @Override
    public String toString() {
        return string;
    }

    @Override
    public QName type() {
        return ValueType.XP_STRING;
    }

    @Override
    public boolean canIgnore(JsonWriterOptions options) {
        return false;
    }

    @Override
    public void write(Writer out) throws IOException {
        JSONValue.writeJSONString(string, out);
    }

    @Override
    public void writeDescriptorString(Writer writer, JsonWriterOptions options) throws IOException {
        write(writer);
    }
}
