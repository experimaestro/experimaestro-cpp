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
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.scripting.Expose;
import sf.net.experimaestro.manager.scripting.Exposed;
import sf.net.experimaestro.utils.db.DbUtils;
import sf.net.experimaestro.utils.db.SQLInsert;
import sf.net.experimaestro.utils.log.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A class that can be locked a given number of times at the same time.
 * <p>
 * This is useful when one wants status limit the number of processes on a host for
 * example
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Exposed
@TypeIdentifier("TOKEN")
public class TokenResource extends Resource {

    final static private Logger LOGGER = Logger.getLogger();

    private static final SQLInsert SQL_INSERT = new SQLInsert("TokenResources", false, "id", "limit", "used");

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

    public TokenResource(long id, Path locator) throws SQLException {
        super(id, locator);

        // Load
        try (PreparedStatement st = Scheduler.prepareStatement("SELECT limit, used FROM TokenResources WHERE id=?")) {
            st.setLong(1, id);
            ResultSet rs = DbUtils.getFirst(st);
            this.limit = rs.getInt(1);
            this.usedTokens = rs.getInt(2);
        }
    }


    /**
     * Creates a new token resource
     *
     * @param path  The token path
     * @param limit The maximum number of tokens
     */
    public TokenResource(Path path, int limit)  {
        super(path);
        this.limit = limit;
        this.usedTokens = 0;
        this.wasBlocking = isBlocking();
        try {
            setState(ResourceState.DONE);
        } catch (SQLException e) {
            throw new XPMRuntimeException(e, "Should not happen - object not in DB");
        }
    }

    @Override
    synchronized protected void save(DatabaseObjects<Resource> resources, Resource old) throws SQLException {
        // Update status
        boolean update = this.inDatabase() || old != null;

        // Save resource
        super.save(resources, old);

        // Insert token resource
        SQL_INSERT.execute(Scheduler.getConnection(), update, getId(), limit, usedTokens);
    }

    public String toDetailedString() {
        return String.format("%s [%d/%d]", super.toDetailedString(), usedTokens, limit);
    }

    public int getLimit() {
        return limit;
    }

    public int getUsedTokens() {
        return usedTokens;
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
    synchronized protected boolean doUpdateStatus() throws Exception {
        LOGGER.debug("Updating token resource");
        int used = 0;
        for (Dependency dependency : getDependencies()) {
            if (dependency.hasLock()) {
                LOGGER.debug("Dependency [%s] has lock", dependency);
                used++;
            }
        }

        if (used != this.usedTokens) {
            this.usedTokens = used;
            return true;
        }

        return false;
    }

    @Expose
    public TokenParameters parameters() {
        return new TokenParameters(this);
    }

    @Override
    public TokenDependency createDependency(DependencyParameters p) {
        int tokens = 1;

        if (p instanceof TokenParameters) {
            tokens = ((TokenParameters) p).tokens;
        }
        return new TokenDependency(this, tokens);
    }

    /**
     * Unlock a resource
     *
     * @return
     * @param tokens
     */
    synchronized void unlock(int tokens) throws SQLException {
        if (usedTokens >= tokens) {
            setValue(usedTokens - 1);
            Scheduler.notifyRunners();
            LOGGER.debug("Releasing %d token (%s/%s)", tokens, usedTokens, limit);
        } else {
            LOGGER.warn("Attempt to release %d non existent tokens (%d/%d)", tokens, usedTokens, limit);

            // Release everything if there is any token left
            if (usedTokens > 0) {
                setValue(0);
                Scheduler.notifyRunners();
            }
        }
    }

    public void takeTokens(int tokens) throws SQLException {
        setValue(usedTokens + tokens);
        LOGGER.debug("Getting one more token (%s/%s)", usedTokens, limit);
    }


    private boolean isBlocking() {
        return usedTokens >= limit;
    }

    /**
     * Sets the number of used tokens
     *
     * @param usedTokens The new value
     * @throws SQLException If something goes wrong
     */
    synchronized private void setValue(final int usedTokens) throws SQLException {
        if (this.usedTokens == usedTokens) return;

        // Set in DB first
        final String s = "UPDATE TokenResources SET used=? WHERE id=?";
        try (final PreparedStatement st = Scheduler.getConnection().prepareStatement(s)) {
            st.setInt(1, usedTokens);
            st.setLong(2, getId());
            st.execute();
        }

        this.usedTokens = usedTokens;
    }

    @Expose("set_limit")
    synchronized public void setLimit(final int limit) throws SQLException {
        if (this.limit == limit) return;

        // Set in DB first
        final String s = "UPDATE TokenResources SET limit=? WHERE id=?";
        try (final PreparedStatement st = Scheduler.getConnection().prepareStatement(s)) {
            st.setInt(1, limit);
            st.setLong(2, getId());
            st.execute();
        }

        // and then change in memory
        if (limit != this.limit) {
            this.limit = limit;
            if (!isBlocking()) {
                // Notify runners if nothing
                Scheduler.notifyRunners();
            }
        }
    }

}
