package sf.net.experimaestro.utils;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 23/1/13
 */
public interface CloseableIterable<T> extends Iterable<T>, AutoCloseable {
}
