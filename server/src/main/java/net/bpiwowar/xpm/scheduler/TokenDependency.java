package net.bpiwowar.xpm.scheduler;

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

import net.bpiwowar.xpm.exceptions.LockException;
import net.bpiwowar.xpm.locks.Lock;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.utils.db.DbUtils;
import net.bpiwowar.xpm.utils.log.Logger;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import static java.lang.String.format;

/**
 * A token dependency
 * <p>
 * The dependency is always satisfied - its satisfaction must be evaluated latter
 */
@TypeIdentifier("TOKEN")
@Exposed
public class TokenDependency extends Dependency {
    final static private Logger LOGGER = Logger.getLogger();

    // Number of requested tokens
    int tokens;

    // The SQL queries
    final private String SELECT_QUERY = "SELECT tokens FROM TokenDependencies WHERE fromId = ? AND toId = ?";
    final private String INSERT_QUERY = "INSERT INTO TokenDependencies(tokens, fromId, toId) VALUES(?,?,?)";
    final private String UPDATE_QUERY = "UPDATE TokenDependencies SET tokens = ? WHERE fromId=? AND toId=?";

    protected TokenDependency(long fromId, long toId, Lock lock, DependencyStatus status) throws SQLException {
        super(fromId, toId, lock, status);

        try (PreparedStatement st = Scheduler.prepareStatement(SELECT_QUERY)) {
            st.setLong(1, from.id());
            st.setLong(2, to.id());
            st.execute();
            DbUtils.processOne(st, true, rs -> this.tokens = rs.getInt(1));
        }
    }

    @Override
    public void save(boolean update) throws SQLException {
        super.save(update);
        int updated = Scheduler.statement(update ? UPDATE_QUERY : INSERT_QUERY)
                .setLong(1, this.tokens)
                .setLong(2, from.id())
                .setLong(3, to.id())
                .executeUpdate();
        LOGGER.debug("TokenDependency %s - updated rows = %d", this, updated);
    }

    public TokenDependency(Resource from, int tokens) {
        super(from);
        this.tokens = tokens;
    }

    @Override
    public String toString() {
        return format("Token[%s;%d]/%s",
                getFrom().toDetailedString(), tokens, super.toString());
    }

    /**
     * A token dependency is always OK. It is just before starting a job that
     * we check the real state with {@link #canLock()}.
     *
     * @return OK_LOCK
     */
    @Override
    protected DependencyStatus _accept() {
        // Always return OK_LOCK for a token dependency
        return DependencyStatus.OK_LOCK;
    }

    /**
     * Checks whether we can obtain a lock for this token dependency
     *
     * @return true if {@linkplain #_lock} will be successful
     */
    public boolean canLock() {
        TokenResource token = (TokenResource) getFrom();
        return token.getUsedTokens() + tokens <= token.getLimit();
    }

    @Override
    protected Lock _lock(String pid) throws LockException {
        TokenResource token = (TokenResource) getFrom();
        if (token.getUsedTokens() + tokens > token.getLimit()) {
            LOGGER.debug("We need %d tokens, which is not possible (used=%s/limit=%s) [token %s for %s]",
                    tokens, token.getUsedTokens(), token.getLimit(), getFrom(), getTo());
            throw new LockException("All the tokens are already taken");
        }

        try {
            token.takeTokens(tokens);
        } catch (SQLException e) {
            throw new LockException(e);
        }
        LOGGER.debug("Taking one token (%s/%s) [token %s for %s]",
                token.getUsedTokens(), token.getLimit(), getFrom(), getTo());
        return new TokenLock(token, tokens);
    }


}
