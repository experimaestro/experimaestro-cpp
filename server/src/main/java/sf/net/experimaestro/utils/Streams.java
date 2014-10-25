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

import sf.net.experimaestro.exceptions.StreamException;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Streams utility functions
 */
public class Streams {
    public interface ExceptionalConsumer<T> {
        void apply(T t) throws Exception;
    }
    /** Propagate exceptions by wrapping them into a runtime exception */
    public static <V> Consumer<V> propagate(ExceptionalConsumer<V> callable) {
        return t -> {
            try {
                callable.apply(t);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new StreamException(e);
            }
        };
    }

    /** Propagate exceptions by wrapping them into a runtime exception */
    public static <V> Consumer<V> ignore(ExceptionalConsumer<V> callable) {
        return t -> {
            try {
                callable.apply(t);
            } catch (Throwable e) {
                // Just ignore
            }
        };
    }

    public interface ExceptionalFunction<R, T> {
        T apply(R r) throws Exception;
    }

    /** Propagate exceptions by wrapping them into a runtime exception */
    public static <R, T> Function<R, T> propagateFunction(ExceptionalFunction<R, T> function) {
        return r -> {
            try {
                return function.apply(r);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new StreamException(e);
            }
        };
    }


    /** Propagate exceptions by wrapping them into a runtime exception */
    public static <R, T> Function<R, T> ignoreFunction(ExceptionalFunction<R, T> callable, T defaultValue) {
        return r -> {
            try {
                return callable.apply(r);
            } catch (Throwable e) {
                // Just ignore
                return defaultValue;
            }
        };
    }
}
