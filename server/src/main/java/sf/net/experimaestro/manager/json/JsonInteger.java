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

import org.json.simple.JSONValue;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.ValueType;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 2/4/13
 */
public class JsonInteger implements Json {
    private final long value;

    public JsonInteger(long value) {
        this.value = value;
    }

    @Override
    public Json clone() {
        return new JsonInteger(value);
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
        return Long.toString(value);
    }

    @Override
    public QName type() {
        return ValueType.XP_INTEGER;
    }

    @Override
    public boolean canIgnore(Set<QName> ignore) {
        return false;
    }

    @Override
    public void writeDescriptorString(Writer writer, Set<QName> ignore) throws IOException {
        write(writer);
    }

    @Override
    public void write(Writer out) throws IOException {
        JSONValue.writeJSONString(value, out);
    }


}
