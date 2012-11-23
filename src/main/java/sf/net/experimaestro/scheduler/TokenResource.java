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

package sf.net.experimaestro.scheduler;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.model.Persistent;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.locks.LockType;
import sf.net.experimaestro.locks.UnlockableException;

/**
 * A class that can be locked by a specified number of resources
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 23/11/12
 */
@Persistent
public class TokenResource extends Resource {
    /** Maximum number of tokens available */
    private int maxTokens;

    /** Number of used tokens */
    private int usedTokens;

    private TokenResource() {
    }

    public TokenResource(Scheduler scheduler, String path, int nbTokens) {
        super(scheduler, scheduler.getConnector(Connectors.XPM_CONNECTOR_ID), path, LockMode.CUSTOM);
        this.maxTokens = nbTokens;
        this.usedTokens = 0;
    }

    @Override
    DependencyStatus accept(LockType locktype) {
        // If we have enough tokens, then hold
        return usedTokens < maxTokens ? DependencyStatus.OK_LOCK : DependencyStatus.WAIT;
    }

    @Override
    synchronized public Lock lock(String pid, LockType dependency) throws UnlockableException {
        if (accept(null) != DependencyStatus.OK_LOCK)
            return null;

        final MyLock lock = new MyLock(this);
        usedTokens++;
        updateDb();
        return lock;
    }

    /**
     * Unlock a resource
     * @return
     */
    synchronized private boolean unlock() {
        usedTokens--;
        return updateDb();
    }


    /**
     * This lock calls {@linkplain sf.net.experimaestro.scheduler.TokenResource#unlock()} when
     * released.
     * TODO: maybe ensure that we only unlock valid locks (using an ID)
     */
    @Persistent
    private static class MyLock implements Lock {
        private String pid;
        private String resourceId;
        transient private TokenResource resource;

        private MyLock() {}

        public MyLock(TokenResource resource) {
            this.resource = resource;
            resourceId = resource.getIdentifier();
        }

        @Override
        public boolean dispose() {
            if (resource != null) {
                resource.unlock();
                resource = null;
                return true;
            }
            return false;
        }

        @Override
        public void changeOwnership(String pid) {
            this.pid = pid;
        }

        @Override
        public void init(Scheduler scheduler) throws DatabaseException {
            this.resource = (TokenResource) scheduler.getResource(ResourceLocator.parse(resourceId));
        }
    }

}
