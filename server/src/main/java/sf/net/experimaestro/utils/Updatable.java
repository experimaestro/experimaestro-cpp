package sf.net.experimaestro.utils;

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

import org.hibernate.sql.Update;

import java.util.HashMap;
import java.util.function.Function;

/**
 * Holds a value and its state - whether it is updatable or not
 */
final public class Updatable<T> {
    /**
     * The held value
     */
    T value;

    /**
     * The status
     */
    boolean modified;

    /**
     * copier
     */
    private final Function<T, T> copier;

    public Updatable(T value) {
        this(value, null, true);
    }

    public Updatable(T value, Function<T, T> copier) {
        this(value, copier, true);
    }

    private Updatable(T value, Function<T, T> copier, boolean modified) {
        this.value = value;
        this.modified = modified;
        this.copier = copier;
    }

    /**
     * Returns a value that can be modified
     */
    public T modify() {
        if (copier != null)
            throw new UnsupportedOperationException("Cannot modify immutable value");

        if (modified) return value;
        value = copier.apply(value);
        return value;
    }

    public T get() {
        return value;
    }

    public Updatable<T> reference() {
        // Clone returns a a shallow copy
        return new Updatable<T>(value, copier, false);
    }

    public void set(T value) {
        this.value = value;
        this.modified = true;
    }

    public static <T> Updatable<T> create(T value) {
        return new Updatable<>(value);
    }

}
