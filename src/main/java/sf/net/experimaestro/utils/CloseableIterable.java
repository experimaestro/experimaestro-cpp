package sf.net.experimaestro.utils;

import sf.net.experimaestro.exceptions.CloseException;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 23/1/13
 */
public interface CloseableIterable<T> extends Iterable<T>, AutoCloseable {
    @Override
    void close() throws CloseException;
}
