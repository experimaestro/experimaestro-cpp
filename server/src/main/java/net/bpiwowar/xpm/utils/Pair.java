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

import java.io.Serializable;
import java.util.Map;

/**
 * @author bpiwowar
 * @date 16/01/2007
 */
public class Pair<T, U> implements Serializable, Map.Entry<T, U> {
    private static final long serialVersionUID = -4235368324324509377L;
    protected T first;
    protected U second;

    public Pair() {
    }

    public Pair(final T x, final U y) {
        this.first = x;
        this.second = y;
    }

    public static <T, U> Pair<T, U> of(T t, U u) {
        return new Pair<T, U>(t, u);
    }

    public final T getFirst() {
        return first;
    }

    public final void setFirst(final T x) {
        this.first = x;
    }

    public final U getSecond() {
        return second;
    }

    public final void setSecond(final U y) {
        this.second = y;
    }

    /* (non-Javadoc)
      * @see java.lang.Object#toString()
      */
    @Override
    public String toString() {
        return String.format("(%s,%s)", first, second);
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((first == null) ? 0 : first.hashCode());
        result = prime * result + ((second == null) ? 0 : second.hashCode());
        return result;
    }

    @Override
    public T getKey() {
        return first;
    }

    @Override
    public U getValue() {
        return second;
    }

    @Override
    public U setValue(U value) {
        U old = value;
        this.second = value;
        return old;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        @SuppressWarnings("unchecked")
        Pair<T, U> other = (Pair<T, U>) obj;
        if (first == null) {
            if (other.first != null)
                return false;
        } else if (!first.equals(other.first))
            return false;
        if (second == null) {
            if (other.second != null)
                return false;
        } else if (!second.equals(other.second))
            return false;
        return true;
    }


}
