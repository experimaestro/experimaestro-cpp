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

/**
 * A class that wraps a result together with a boolean
 * indicating the success of failure of the operation
 *
 * @param <T> The wrapped result
 */
public class WrappedResult<T> {
    boolean success;
    T t;

    public WrappedResult(boolean success, T t) {
        this.success = success;
        this.t = t;
    }

    public boolean isSuccess() {
        return success;
    }

    public T get() {
        return t;
    }
}
