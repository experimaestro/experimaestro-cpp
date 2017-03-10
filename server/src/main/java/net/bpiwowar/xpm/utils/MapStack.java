package net.bpiwowar.xpm.utils;

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

import com.google.common.collect.AbstractIterator;

import java.util.*;

/**
 * A Map backed up by a stack of maps
 * <p/>
 * New values are inserted on the top of the stack
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class MapStack<Key, Value> extends AbstractMap<Key, Value> {
    MapStack<Key, Value> previous;
    final Map<Key, Value> map = new HashMap<>();

    public MapStack(MapStack<Key, Value> previous) {
        this.previous = previous;
    }

    public MapStack() {
    }

    @Override
    public Set<Entry<Key, Value>> entrySet() {
        return new AbstractSet<Entry<Key, Value>>() {
            @Override
            public Iterator<Entry<Key, Value>> iterator() {
                return new AbstractIterator<Entry<Key, Value>>() {
                    Iterator<Entry<Key, Value>> iterator = map.entrySet().iterator();
                    final MapStack<Key, Value> current = MapStack.this;

                    @Override
                    protected Entry<Key, Value> computeNext() {
                        while (!iterator.hasNext()) {
                            if (current.previous == null)
                                return endOfData();
                            iterator = current.map.entrySet().iterator();
                        }
                        return iterator.next();
                    }
                };
            }

            @Override
            public int size() {
                int size = 0;
                for (MapStack<Key, Value> current = MapStack.this; current != null; current = current.previous) {
                    size += current.map.size();
                }
                return size;
            }
        };
    }

    @Override
    public Value get(Object key) {
        for (MapStack<Key, Value> map = this; map != null; map = map.previous)
            if (map.map.containsKey(key))
                return map.map.get(key);
        return null;
    }


    @Override
    public boolean containsKey(Object key) {
        for (MapStack<Key, Value> map = this; map != null; map = map.previous)
            if (map.map.containsKey(key))
                return true;
        return false;
    }

    @Override
    public Value put(Key key, Value value) {
        for (MapStack<Key, Value> map = this; map != null; map = map.previous)
            if (map.map.containsKey(key))
                return map.map.put(key, value);

        return map.put(key, value);
    }

    public MapStack newMap() {
        return new MapStack(this);
    }

    public Map<Key, Value> previous() {
        return previous;
    }

    public void putNoOverwrite(Key key, Value value) {
        for (MapStack<Key, Value> map = this; map != null; map = map.previous)
            if (map.map.containsKey(key))
                return;

        map.put(key, value);
    }
}
