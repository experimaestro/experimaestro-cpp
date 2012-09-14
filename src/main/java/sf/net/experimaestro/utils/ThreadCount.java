/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.utils;

import org.apache.log4j.Level;
import sf.net.experimaestro.utils.log.Logger;


/**
 * Useful to wait until some threads have finished
 *
 * @author bpiwowar
 */
public class ThreadCount {
    final static Logger logger = Logger.getLogger();
    volatile int counter;


    public final synchronized void add(int i) {
        counter += i;
    }

    public synchronized void add() {
        counter++;
    }

    public synchronized void del() {
        counter--;
        notify();
    }

    public synchronized int getCount() {
        return counter;
    }

    /**
     * Wait until the count is zero
     */
    public void resume() {
        resume(0);
    }

    /**
     * Wait until the count is less than a given value
     */
    public void resume(int n) {
        while (getCount() > n)
            try {
                synchronized (this) {
                    wait();
                }
            } catch (IllegalMonitorStateException e) {
                logger.warn("Illegal monitor exception while sleeping (SHOULD NOT HAPPEN)", e);
            } catch (Exception e) {
                logger.debug("Interrupted while sleeping: %s", e.toString());
                if (logger.isDebugEnabled()) {
                    logger.printException(Level.DEBUG, e);
                }
            }
    }

}
