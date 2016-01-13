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

    public synchronized void close() {
        LOGGER.debug("Cleaner is cleaning %d elements", list.size());
        final Iterator<AutoCloseable> iterator = list.iterator();
        while (iterator.hasNext()) {
            final AutoCloseable value = iterator.next();
            try {
                value.close();
            } catch (Exception e) {
                LOGGER.error(e, "Error while closing %s", value);
            }
        }
        list.clear();
    }
}
