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

import java.io.File;

/**
 * Look for file changes
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class WatchFileMonitor {
    static public enum Mode {
        MODIFIED, DELETED
    }

    private File file;
    Mode mode;
    private final long pollingInterval;

    public WatchFileMonitor(File file, Mode mode, long pollingInterval) {
        this.file = file;
        this.mode = mode;
        this.pollingInterval = pollingInterval;

    }

    public WatchFileMonitor(File file, Mode mode) {
        this(file, mode, 5000);
    }

    public void take() {
        while (!good()) {
            synchronized (this) {
                try {
                    wait(pollingInterval);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    private boolean good() {
        switch (mode) {
            case DELETED:
                return !file.exists();
            case MODIFIED:
                throw new RuntimeException("Not implemented");
        }
        return false;
    }
}
