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

import org.json.simple.JSONObject;
import sf.net.experimaestro.exceptions.LockException;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.utils.log.Logger;

import java.io.IOException;
import java.nio.file.Path;

/**
 * A class that can be locked a given number of times at the same time.
 * <p/>
 * This is useful when one wants to limit the number of processes on a host for
 * example
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 23/11/12
 */
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


    protected TokenResource() {
    }

    /**
     * Creates a new token resource
     *  @param data      The resource data
     * @param limit     The maximum number of tokens*/
    public TokenResource(Path data, int limit) {
        this.limit = limit;
        this.usedTokens = 0;
        setState(ResourceState.DONE);
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
        storeState(true);
    }

    @Override
    public JSONObject toJSON() throws IOException {
        JSONObject info = super.toJSON();
        JSONObject tokenInfo = new JSONObject();
        tokenInfo.put("used", usedTokens);
        tokenInfo.put("limit", limit);
        info.put("tokens", tokenInfo);
        return info;
    }

    @Override
    synchronized protected boolean doUpdateStatus(boolean store) throws Exception {
        LOGGER.debug("Updating token resource");
        int used = 0;
        for (Dependency dependency : getDependentResources()) {
            if (dependency.hasLock()) {
                LOGGER.debug("Dependency [%s] has lock", dependency);
                used++;
            }
        }

        if (used != this.usedTokens) {
            this.usedTokens = used;
            if (store) {
                Scheduler.get().store(this, true);
            }
            return true;
        }

        return false;
    }


    @Override
    public TokenDependency createDependency(Object values) {
        return new TokenDependency(this);
    }

    /**
     * Unlock a resource
     *
     * @return
     */
    synchronized private void unlock() {
        usedTokens--;
        LOGGER.debug("Releasing one token (%s/%s)", usedTokens, limit);
        storeState(true);
    }

    /**
     * A token dependency
     */
    static public class TokenDependency extends Dependency {

        protected TokenDependency() {
        }

        public TokenDependency(Resource from) {
            super(from);
        }

        @Override
        public String toString() {
            return "Token/" + super.toString();
        }

        @Override
        protected DependencyStatus _accept() {
            TokenResource token = (TokenResource) getFrom();
            return token.usedTokens < token.limit ? DependencyStatus.OK_LOCK : DependencyStatus.WAIT;
        }

        @Override
        protected Lock _lock(String pid) throws LockException {
            TokenResource token = (TokenResource) getFrom();
            synchronized (token) {
                if (token.usedTokens >= token.limit)
                    throw new LockException("All the tokens are already taken");

                token.usedTokens++;
                LOGGER.debug("Taking one token (%s/%s)", token.usedTokens, token.limit);
                token.storeState(true);

                return new TokenLock(token);
            }
        }
    }

    /**
     * This lock calls {@linkplain sf.net.experimaestro.scheduler.TokenResource#unlock()} when
     * released.
     * TODO: maybe ensure that we only unlock valid locks (using an ID)
     */
    private static class TokenLock extends Lock {
        private String pid;
        private String resourceId;
        transient private TokenResource resource;

        private TokenLock() {
        }

        public TokenLock(TokenResource resource) {
            this.resource = resource;
            resourceId = resource.getIdentifier();
        }


        @Override
        public void close() {
            if (resource != null) {
                resource.unlock();
                resource = null;
            }
        }

        @Override
        public void changeOwnership(String pid) {
            this.pid = pid;
        }

    }

}
