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

package sf.net.experimaestro.locks;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.model.Persistent;
import sf.net.experimaestro.scheduler.Resource;
import sf.net.experimaestro.scheduler.ResourceLocator;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.log.Logger;

/**
 * Defines a status-based lock
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Persistent
public class StatusLock implements Lock {
    final static private Logger LOGGER = Logger.getLogger();
    private String pid;
    private boolean writeAccess;

    ResourceLocator resourceId;

    /** The locked resource */
    transient Resource resource;

    private StatusLock() {
    }

    /**
     * Status lock initialization
     *
     * @param resource
     * @param pid
     * @param writeAccess
     * @throws UnlockableException
     */
    public StatusLock(final Resource resource, String pid, final boolean writeAccess)
            throws UnlockableException {
        this.pid = pid;
        this.resource = resource;
        this.resourceId = resource.getLocator();
        this.writeAccess = writeAccess;
        resource.updateStatusFile(null, pid, writeAccess);
    }

    /**
     * Change the PID in the status file
     *
     * @throws sf.net.experimaestro.locks.UnlockableException
     */
    public void changePID(String pid) throws UnlockableException {
        resource.updateStatusFile(this.pid, pid, writeAccess);
        this.pid = pid;
    }

    /*
       * (non-Javadoc)
       *
       * @see bpiwowar.expmanager.locks.Lock#dispose()
       */
    public boolean dispose() {
        try {
            resource.updateStatusFile(pid, null, writeAccess);
        } catch (UnlockableException e) {
            return false;
        }
        return true;
    }

    /*
       * (non-Javadoc)
       *
       * @see bpiwowar.expmanager.locks.Lock#changeOwnership(int)
       */
    public void changeOwnership(String pid) {
        try {
            resource.updateStatusFile(this.pid, pid, writeAccess);
        } catch (UnlockableException e) {
            return;
        }

        this.pid = pid;
    }

    @Override
    public void init(Scheduler scheduler) throws DatabaseException {
        resource = scheduler.getResource(resourceId);
    }




}
