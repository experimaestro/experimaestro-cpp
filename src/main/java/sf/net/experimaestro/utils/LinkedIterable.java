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

import java.util.Iterator;

/**
 * Makes an iterable from an iterator
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 13/3/13
 */
public class LinkedIterable<T> implements Iterable<T> {

    boolean started = false;

    static public class LinkedValue<T> {
        T value;
        LinkedValue<T> next;

        public LinkedValue(T value, LinkedValue current) {
            this.value = value;
            if (current != null)
                current.next = this;
        }
    }

    private final Iterator<T> iterator;

    public LinkedIterable(Iterator<T> iterator) {
        this.iterator = iterator;
    }

    public boolean started() {
        return started;
    }

    @Override
    public Iterator<T> iterator() {
        if (started)
            return null;

        return new AbstractIterator<T>() {
            LinkedValue<T> current;

            @Override
            protected T computeNext() {
                if (current != null && current.next != null) {
                    current = current.next;
                    return current.value;
                }

                started = true;
                if (!iterator.hasNext())
                    return endOfData();

                current = new LinkedValue(iterator.next(), current);
                return current.value;
            }
        };
    }
}
