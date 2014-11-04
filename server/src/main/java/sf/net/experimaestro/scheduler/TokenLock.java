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

import sf.net.experimaestro.locks.Lock;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * This lock calls {@linkplain TokenResource#unlock()} when
 * released.
 * TODO: maybe ensure that we only unlock valid locks (using an ID)
 */
@Entity
@DiscriminatorValue("token")
class TokenLock extends Lock {
    private String pid;
    private String resourceId;
    transient private TokenResource resource;

    protected TokenLock() {
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
