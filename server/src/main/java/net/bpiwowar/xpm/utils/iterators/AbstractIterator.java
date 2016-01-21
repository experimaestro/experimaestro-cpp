package net.bpiwowar.xpm.utils.iterators;

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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @deprecated Use Guava {@link com.google.common.collect.AbstractIterator}
 * @param <E> The iterator value
 */
@Deprecated
abstract public class AbstractIterator<E> implements Iterator<E> {
    protected E value;
    byte status = -1;

    /**
     * Stores a new element in value
     *
     * @return true if there was a new element, false otherwise
     */
    protected abstract boolean storeNext();

    final protected void store(E e) {
        this.value = e;
    }

    final public boolean hasNext() {
        if (status == -1)
            status = (byte) (storeNext() ? 1 : 0);
        return status == 1;
    }

    @Override
    final public E next() {
        if (!hasNext())
            throw new NoSuchElementException();
        E next = value;
        status = -1;
        return next;
    }

    final public void remove() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("serial")
    static class EndOfStream extends Throwable {
    }

}
