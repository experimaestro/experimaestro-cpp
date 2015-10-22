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

import java.sql.SQLException;

/**
 * This lock calls {@linkplain TokenResource#unlock(int)} when
 * released.
 */
@TypeIdentifier("token")
public class TokenLock extends Lock {
    // The resource
    private TokenResource resource;

    // Tokens to
    int tokens = 1;

    /// From database
    public TokenLock(long id) {
        super(id);
    }

    public TokenLock(TokenResource resource, int tokens) {
        this.resource = resource;
        this.tokens = tokens;
    }


    @Override
    public void doClose() throws LockException {
        if (resource != null) {
            try {
                resource.unlock(tokens);
            } catch (SQLException e) {
                throw new LockException(e);
            }
            resource = null;
        }
    }

    @Override
    public void changeOwnership(String pid) throws LockException {
        // TODO: maybe ensure that we only unlock valid locks (using an ID)
    }
}
