/**
 *
 */
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

import net.bpiwowar.xpm.utils.iterators.AbstractIterator;

import java.lang.reflect.Array;
import java.util.Iterator;

/**
 * This produces a cartesian product over all the possible combinations
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class CartesianProduct<T> implements Iterable<T[]> {
    private final Iterable<? extends T>[] iterables;
    private final Class<T> klass;

    public CartesianProduct(Class<T> klass, Iterable<? extends T>... iterables) {
        this.klass = klass;
        this.iterables = iterables;
    }

    public static <T> Iterable<T[]> of(Class<T> aClass, Iterable<? extends T>... values) {
        return new CartesianProduct<>(aClass, values);
    }

    @Override
    public Iterator<T[]> iterator() {
        @SuppressWarnings("unchecked")
        final Iterator<? extends T>[] iterators = new Iterator[iterables.length];

        return new AbstractIterator<T[]>() {
            boolean eof = iterables.length == 0;

            @Override
            protected boolean storeNext() {
                if (eof)
                    return false;

                if (value == null) {
                    // Initialisation
                    // @SuppressWarnings("unchecked")
                    value = (T[]) Array.newInstance(klass, iterables.length);

                    for (int i = 0; i < iterables.length; i++) {
                        iterators[i] = iterables[i].iterator();
                        if (!iterators[i].hasNext()) {
                            eof = true;
                            return false;
                        }
                        value[i] = iterators[i].next();
                    }
                } else {
                    // Next
                    for (int i = 0; i < iterables.length; i++) {
                        if (!iterators[i].hasNext()) {
                            if (iterables.length - 1 == i) {
                                eof = true;
                                return false;
                            }
                            iterators[i] = iterables[i].iterator();
                            value[i] = iterators[i].next();
                        } else {
                            // OK - we have found the right iterator
                            value[i] = iterators[i].next();
                            break;
                        }
                    }
                }

                return true;
            }
        };
    }
}