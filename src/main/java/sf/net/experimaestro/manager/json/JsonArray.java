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

import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.utils.Output;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import static java.lang.String.format;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 1/4/13
 */
public class JsonArray extends ArrayList<Json> implements Json {
    public JsonArray(int initialCapacity) {
        super(initialCapacity);
    }

    public JsonArray() {
    }

    public JsonArray(Collection<? extends Json> c) {
        super(c);
    }

    @Override
    public String toString() {
        return format("[%s]", Output.toString(", ", this));
    }

    @Override
    public void write(Writer out) throws IOException {
        out.write('[');
        boolean first = true;
        for (Json json : this) {
            if (first)
                first = false;
            else
                out.write(", ");
            json.write(out);
        }
        out.write(']');
    }

    @Override
    public Json clone() {
        JsonArray array = new JsonArray();
        for (Json json : this)
            array.add(json.clone());
        return array;
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public Object get() {
    	return this;
    }

    @Override
    public QName type() {
        return Manager.XP_ARRAY;
    }

    @Override
    public boolean canIgnore(Set<QName> ignore) {
        return size() == 0;
    }

    @Override
    public void writeDescriptorString(Writer out, Set<QName> ignore) throws IOException {
        out.write('[');
        boolean first = true;
        for (Json json : this) {
            if (first)
                first = false;
            else
                out.write(", ");
            json.writeDescriptorString(out, ignore);
        }
        out.write(']');
    }
}
