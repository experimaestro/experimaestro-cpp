package sf.net.experimaestro.plan;

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

import com.google.common.collect.ImmutableSet;

import java.util.Iterator;
import java.util.Map;

abstract public class Node implements Iterable<Map<String, Value>> {
    static final public Node EMPTY = new Node() {
        @Override
        public Iterator<Map<String, Value>> iterator() {
            return ImmutableSet.<Map<String, Value>>of().iterator();
        }
    };
}
