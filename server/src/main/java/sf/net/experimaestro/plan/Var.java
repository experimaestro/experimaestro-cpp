/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.plan;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableMap;
import sf.net.experimaestro.utils.Output;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class Var extends Node {
    String name;
    ArrayList<Value> values = new ArrayList<>();

    public String toString() {
        return String.format("%s=[%s]", name, Output.toString("],[", values));
    }

    @Override
    public Iterator<Map<String, Value>> iterator() {

        return new AbstractIterator<Map<String, Value>>() {
            Iterator<Value> it = values.iterator();

            @Override
            protected Map<String, Value> computeNext() {
                if (!it.hasNext())
                    return endOfData();
                return ImmutableMap.of(name, it.next());
            }
        };
    }
}