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

import sf.net.experimaestro.exceptions.LockException;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.utils.log.Logger;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import static java.lang.String.format;

/**
 * A token dependency
 *
 * The dependency is always satisfied - its satisfaction must be evaluated latter
 */
@Entity
@DiscriminatorValue("token")
public class TokenDependency extends Dependency {
    final static private Logger LOGGER = Logger.getLogger();

    protected TokenDependency() {
    }

    public TokenDependency(Resource from) {
        super(from);
    }

    @Override
    public String toString() {
        return format("Token[%s]/%s", from, super.toString());
    }

    /**
     * A token dependency is always OK. It is just before starting a job that
     * we check the real state with {@link #canLock()}.
     * @return OK_LOCK
     */
    @Override
    protected DependencyStatus _accept() {
        // Always return OK_LOCK for a token dependency
        return DependencyStatus.OK_LOCK;
    }

    /**
     * Checks whether we can obtain a lock for this token dependency
     * @return true if {@linkplain #_lock} will be successful
     */
    public boolean canLock() {
        TokenResource token = (TokenResource) getFrom();
        return token.getUsedTokens() < token.getLimit();
    }

    @Override
    protected Lock _lock(String pid) throws LockException {
        TokenResource token = (TokenResource) getFrom();
        if (token.getUsedTokens() >= token.getLimit()) {
            LOGGER.debug("All tokens are already taken (%s/%s) [token %s for %s]",
                    token.getUsedTokens(), token.getLimit(), getFrom(), getTo());
            throw new LockException("All the tokens are already taken");
        }

        token.increaseUsedTokens();
        LOGGER.debug("Taking one token (%s/%s) [token %s for %s]",
                token.getUsedTokens(), token.getLimit(), getFrom(), getTo());
        return new TokenLock(token);
    }


}
