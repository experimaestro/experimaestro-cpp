package sf.net.experimaestro.utils;

/**
 *
 */
public interface ExceptionalConsumer<T> {
    void apply(T t) throws Exception;
}
