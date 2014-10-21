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

package sf.net.experimaestro.utils;

import com.google.common.collect.AbstractIterator;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Caches the result of an iterator in order to make an iterable from it
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 13/3/13
 */
public class CachedIterable<T> implements Iterable<T> {
    private final Iterator<T> iterator;
    ArrayList<T> cache = new ArrayList<>();

    public CachedIterable(Iterator<T> iterator) {
        this.iterator = iterator;
    }

    @Override
    public Iterator<T> iterator() {
        return new AbstractIterator<T>() {
            int index = 0;

            @Override
            protected T computeNext() {
                if (index >= cache.size()) {
                    if (!iterator.hasNext()) {
                        return endOfData();
                    }
                    cache.add(iterator.next());
                }

                // Return current object
                return cache.get(index++);
            }
        };
    }
}
