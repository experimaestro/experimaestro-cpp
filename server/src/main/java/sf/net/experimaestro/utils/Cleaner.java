package sf.net.experimaestro.utils;

import sf.net.experimaestro.utils.log.Logger;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A cleaner closes all the resources when finished
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Cleaner implements AutoCloseable {
    final private static Logger LOGGER = Logger.getLogger();

    private ArrayList<AutoCloseable> list = new ArrayList<>();

    synchronized public void register(AutoCloseable closeable) {
        list.add(closeable);
    }

    synchronized public void unregister(AutoCloseable closeable) {
        final Iterator<AutoCloseable> iterator = list.iterator();
        while (iterator.hasNext()) {
            final AutoCloseable value = iterator.next();
            if (value == closeable) {
                iterator.remove();
                break;
            }
        }
    }

    public synchronized void close() throws Exception {
        LOGGER.debug("Cleaner is cleaning %d elements", list.size());
        final Iterator<AutoCloseable> iterator = list.iterator();
        while (iterator.hasNext()) {
            final AutoCloseable value = iterator.next();
            value.close();
            iterator.remove();
        }
    }
}
