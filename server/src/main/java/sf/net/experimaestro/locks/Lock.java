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
import sf.net.experimaestro.scheduler.Scheduler;

import javax.persistence.*;

/**
 * A lock that can be removed.
 * <p/>
 * The lock is taken during the object construction which is dependent on the
 * actual {@link Lock} implementation.
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Entity(name = "locks")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "locks")
public abstract class Lock implements AutoCloseable {
    @Id
    private long id;

    @Override
    abstract public void close() throws RuntimeException;

    /**
     * Change ownership
     *
     * @param pid The new owner PID
     */
    public abstract void changeOwnership(String pid) throws LockException;
}

