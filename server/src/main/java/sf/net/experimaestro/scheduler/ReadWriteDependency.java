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
import sf.net.experimaestro.manager.scripting.Exposed;
import sf.net.experimaestro.utils.log.Logger;


/**
 * One can write, many can read
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@TypeIdentifier("READ-WRITE")
@Exposed
public class ReadWriteDependency extends Dependency {
    static final private Logger LOGGER = Logger.getLogger();

    public ReadWriteDependency(long fromId, long toId, DependencyStatus status) {
        super(fromId, toId, status);
    }

    public ReadWriteDependency(Resource from) {
        super(from);
    }

    @Override
    public String toString() {
        return super.toString() + "/RW";
    }

    @Override
    protected DependencyStatus _accept() {
        // The file was generated, so it is just a matter of locking
        return DependencyStatus.OK_LOCK;
    }

    @Override
    protected Lock _lock(String pid) throws LockException {
        // Retrieve data about resource
        Resource resource = getFrom();

        return new StatusLock(resource.getPath(), pid, false);
    }
}
