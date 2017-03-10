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

import net.bpiwowar.xpm.exceptions.WrappedException;
import net.bpiwowar.xpm.exceptions.XPMAssertionError;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Streams utility functions
 */
public class Functional {

    /**
     * Propagate exceptions by wrapping them into a runtime exception
     */
    public static <V> Consumer<V> propagate(ExceptionalConsumer<V> callable) {
        return t -> {
            try {
                callable.apply(t);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new WrappedException(e);
            }
        };
    }

    /**
     * Propagate exceptions by wrapping them into a runtime exception
     */
    public static <V> Consumer<V> ignore(ExceptionalConsumer<V> callable) {
        return t -> {
            try {
                callable.apply(t);
            } catch (Throwable e) {
                // Just ignore
            }
        };
    }

    public interface ExceptionalBiConsumer<T, U> {
        void apply(T t, U u) throws Exception;
    }

    /**
     * Propagate exceptions by wrapping them into a runtime exception
     */
    public static <T, U> BiConsumer<T, U> propagate(ExceptionalBiConsumer<T, U> callable) {
        return (t, u) -> {
            try {
                callable.apply(t, u);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new WrappedException(e);
            }
        };
    }

    public interface ExceptionalFunction<R, T> {
        T apply(R r) throws Exception;
    }

    /**
     * Propagate exceptions by wrapping them into a runtime exception
     */
    public static <R, T> Function<R, T> propagateFunction(ExceptionalFunction<R, T> function) {
        return r -> {
            try {
                return function.apply(r);
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new WrappedException(e);
            }
        };
    }


    /**
     * Propagate exceptions by wrapping them into a runtime exception
     */
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

    public static Runnable runnable(ExceptionalRunnable p) {
        return () -> {
            try {
                p.apply();
            } catch (Throwable throwable) {
                throw new XPMAssertionError(throwable, "Should not have thrown an exception");
            }
        };
    }


    public static void shouldNotThrow(ExceptionalRunnable p) {
        try {
            p.apply();
        } catch (Throwable throwable) {
            throw new XPMAssertionError(throwable, "Should not have thrown an exception");
        }
    }
}
