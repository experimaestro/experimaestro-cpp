package sf.net.experimaestro.scheduler;

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

import org.json.simple.JSONObject;
import sf.net.experimaestro.utils.log.Logger;

import javax.persistence.*;
import java.io.IOException;

/**
 * A class that can be locked a given number of times at the same time.
 * <p>
 * This is useful when one wants status limit the number of processes on a host for
 * example
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Entity
// FIXME: change name (when branch is master)
public class TokenResource extends Token {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * Maximum number of tokens available
     */
    private int limit;

    /**
     * Number of used tokens
     */
    private int usedTokens;

    /**
     * Keeps track of the old state
     */
    transient boolean wasBlocking;



    protected TokenResource() {
    }

    @PostLoad
    void postLoad() {
        wasBlocking = isBlocking();
    }

    /**
     * Creates a new token resource
     *
     * @param identifier The token path
     * @param limit The maximum number of tokens
     */
    public TokenResource(String identifier, int limit) {
        super(identifier);
        this.limit = limit;
        this.usedTokens = 0;
        this.wasBlocking = isBlocking();
    }

    /**
     * Set the new limit that will take effect at the next locking request (does not
     * invalidate current locks)
     *
     * @param limit The new limit
     * @return true if the limit was changed
     */
    public boolean setLimit(int limit) {
        if (this.limit == limit) {
            return false;
        }
        this.limit = limit;
        return true;
    }

    public int getLimit() {
        return limit;
    }

    public int getUsedTokens() {
        return usedTokens;
    }

    public JSONObject toJSON() throws IOException {
        JSONObject info = super.toJSON();
        JSONObject tokenInfo = new JSONObject();
        tokenInfo.put("used", usedTokens);
        tokenInfo.put("limit", limit);
        info.put("tokens", tokenInfo);
        return info;
    }

    @Override
    synchronized protected boolean doUpdateStatus() throws Exception {
        return false;
    }


    @Override
    public CountTokenDependency createDependency(Object values) {
        return new CountTokenDependency(this);
    }

    /**
     * Unlock a resource
     *
     * @return
     */
    synchronized void unlock() {
        --usedTokens;
        LOGGER.debug("Releasing one token (%s/%s) [version %d]", usedTokens, limit, version);
    }

    public void increaseUsedTokens() {
        ++usedTokens;
        LOGGER.debug("Getting one more token (%s/%s) [version %d]", usedTokens, limit, version);
    }

    @Override
    public void stored() {
        // Notify scheduler state has changed
        if (wasBlocking ^ isBlocking()) {
            LOGGER.debug("Token %s changed of state (%s to %s): notifying dependencies",
                    this, wasBlocking, isBlocking());
            Transaction.run((em, t) -> notifyDependencies(t, em));
        }
    }

    private boolean isBlocking() {
        return usedTokens >= limit;
    }

    private String id;

}
