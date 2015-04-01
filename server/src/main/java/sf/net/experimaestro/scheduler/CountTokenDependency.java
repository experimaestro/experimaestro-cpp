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

/**
 * A token dependency
 */
@Entity
@DiscriminatorValue("token")
public class CountTokenDependency extends TokenRequirement {
    final static private Logger LOGGER = Logger.getLogger();

    protected CountTokenDependency() {
    }

    public CountTokenDependency(Token from) {
        super(from);
    }

    @Override
    public String toString() {
        return "Token/" + super.toString();
    }


    @Override
    protected Lock _lock(String pid) throws LockException {
        TokenResource token = (TokenResource) getToken();
        if (token.getUsedTokens() >= token.getLimit()) {
            LOGGER.debug("All tokens are already taken (%s/%s) [token %s for %s]",
                    token.getUsedTokens(), token.getLimit(), getToken(), getTo());
            throw new LockException("All the tokens are already taken");
        }

        token.increaseUsedTokens();
        LOGGER.debug("Taking one token (%s/%s) [token %s for %s]",
                token.getUsedTokens(), token.getLimit(), getToken(), getTo());
        return new TokenLock(token);
    }
}
