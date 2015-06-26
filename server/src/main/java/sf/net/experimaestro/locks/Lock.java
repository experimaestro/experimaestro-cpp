package sf.net.experimaestro.locks;

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
import sf.net.experimaestro.manager.scripting.Exposed;
import sf.net.experimaestro.scheduler.Identifiable;
import sf.net.experimaestro.scheduler.Locks;
import sf.net.experimaestro.scheduler.Resources;
import sf.net.experimaestro.scheduler.Scheduler;

/**
 * A lock that can be removed.
 * <p/>
 * The lock is taken during the object construction which is dependent on the
 * actual {@link Lock} implementation.
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Exposed
public abstract class Lock implements AutoCloseable, Identifiable {
    private Long id;

    @Override
    abstract public void close() throws LockException;

    /**
     * Change ownership
     *
     * @param pid The new owner PID
     */
    public abstract void changeOwnership(String pid) throws LockException;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    synchronized final public void save() {
        save(Scheduler.get().locks());
    }

    protected void save(Locks locks) {
        throw new UnsupportedOperationException();
    }


}

