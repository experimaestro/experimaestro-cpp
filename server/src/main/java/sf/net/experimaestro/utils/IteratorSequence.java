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

package sf.net.experimaestro.utils;

import sf.net.experimaestro.utils.iterators.AbstractIterator;

import java.util.Iterator;

/**
 * Glue a series of iterators together to form an iterator over the whole
 * sequence
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class IteratorSequence<T> extends AbstractIterator<T> {
    private final Iterator<T>[] iterators;
    int i = 0;

    public IteratorSequence(Iterator<T>... iterators) {
        this.iterators = iterators;

    }

    public static <T> IteratorSequence<T> create(Iterator<T>... iterators) {
        return new IteratorSequence<T>(iterators);
    }

    @Override
    protected boolean storeNext() {
        while (i < iterators.length) {
            if (iterators[i].hasNext()) {
                value = iterators[i].next();
                return true;
            }
            i++;
        }
        return false;
    }

}
