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

import org.apache.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Useful to wait until some threads have finished
 *
 * @author bpiwowar
 */
public class ThreadCount {
    private final static Logger LOGGER = LogManager.getFormatterLogger();

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
        resume(0, 0, false);
    }

    /**
     * Wait until the count is less than a given value
     *
     * @param n       The minimum value of the counter
     * @param timeout The timeout (in ms) after which we exit
     * @param reset   Resets the time out each time the counter is changed
     */
    public void resume(int n, long timeout, boolean reset) {
        long endTimeout = timeout > 0 ? System.currentTimeMillis() + timeout : Long.MAX_VALUE;
        LOGGER.debug("Waiting for counter to be <= %d (%d) - time-out to %dms from now", n, getCount(), endTimeout - System.currentTimeMillis());

        while (getCount() > n && System.currentTimeMillis() < endTimeout) {
            if (reset) {
                long current = System.currentTimeMillis();
                endTimeout = timeout > 0 ? current + timeout : Long.MAX_VALUE;
                LOGGER.debug("Time-out reset to %dms from now [%s]", endTimeout - current, current);
            }
            try {
                synchronized (this) {
                    wait(timeout);
                }
            } catch (IllegalMonitorStateException e) {
                LOGGER.warn("Illegal monitor exception while sleeping (SHOULD NOT HAPPEN)", e);
            } catch (Exception e) {
                LOGGER.debug("Interrupted while sleeping: %s", e);
            }
        }
    }
}
