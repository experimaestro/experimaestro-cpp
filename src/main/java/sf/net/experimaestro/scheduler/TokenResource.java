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
import sf.net.experimaestro.utils.log.Logger;

import java.io.PrintWriter;

/**
 * A class that can be locked a given number of times at the same time.
 * <p/>
 * This is useful when one wants to limit the number of processes on a host for
 * example
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 23/11/12
 */
@Persistent
public class TokenResource extends Resource {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * Maximum number of tokens available
     */
    private int limit;

    /**
     * Number of used tokens
     */
    private int usedTokens;

    private TokenResource() {
    }

    public TokenResource(Scheduler scheduler, String path, int limit) {
        super(scheduler, scheduler.getConnector(Connectors.XPM_CONNECTOR_ID), path, LockMode.CUSTOM);
        this.limit = limit;
        this.usedTokens = 0;
        state = ResourceState.DONE;
    }

    /**
     * Set the new limit that will take effect at the next locking request (does not
     * invalidate current locks)
     *
     * @param limit The new limit
     */
    synchronized public void setLimit(int limit) {
        if (this.limit == limit) return;

        this.limit = limit;
        updateDb();
    }

    @Override
    DependencyStatus accept(LockType locktype) {
        // If we have enough tokens, then hold
        return usedTokens < limit ? DependencyStatus.OK_LOCK : DependencyStatus.WAIT;
    }

    @Override
    synchronized public Lock lock(String pid, LockType dependency) throws UnlockableException {
        if (accept(null) != DependencyStatus.OK_LOCK)
            return null;
        usedTokens++;

        final MyLock lock = new MyLock(this);
        updateDb();
        return lock;
    }

    /**
     * Unlock a resource
     *
     * @return
     */
    synchronized private boolean unlock() {
        usedTokens--;
        return updateDb();
    }

    @Override
    public void printXML(PrintWriter out, PrintConfig config) {
        super.printXML(out, config);
        out.format("<div>Used tokens: %d out of %d</div>", usedTokens, limit);
    }

    @Override
    protected boolean doUpdateStatus() throws Exception {
        return false;
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

        private MyLock() {
        }

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
