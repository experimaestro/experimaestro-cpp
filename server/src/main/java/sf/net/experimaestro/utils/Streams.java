package sf.net.experimaestro.utils;

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
