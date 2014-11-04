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

import javax.persistence.*;
import java.io.Serializable;

/**
 * What is the state of a dependency.
 * This class stores the previous state
 * (satisfied or not) in order to updateFromStatusFile the number of blocking resources
 */
@Entity(name = "dependencies")
@Table(name = "dependencies")
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
abstract public class Dependency implements Serializable {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * The state of this dependency
     */
    DependencyStatus status;


    @ManyToOne
    @JoinColumn(name = "fromId")
    @Id
    Resource from;

    @Id
    @ManyToOne
    @JoinColumn(name = "toId")
    Resource to;

    /**
     * The lock (or null if no lock taken)
     */
    @OneToOne(optional = true)
    private Lock lock;

    protected Dependency() {
    }

    public Dependency(Resource from) {
        this.from = from;
    }

    public boolean hasLock() {
        return lock != null;
    }

    public Resource getFrom() {
        return from;
    }

    public Resource getTo() {
        return to;
    }

    @Override
    public String toString() {
        return String.format("Dep[R%s-R%s]; %s; %b", from, to, status, hasLock());
    }

    /**
     * Can the dependency be accepted?
     *
     * @return {@link DependencyStatus#OK} if the dependency is satisfied,
     * {@link DependencyStatus#WAIT} if it can be satisfied one day
     * {@link DependencyStatus#HOLD} if it can be satisfied after an external change
     * {@link DependencyStatus#ERROR} if it cannot be satisfied
     */
    protected DependencyStatus accept() {
        LOGGER.debug("From [%d] is in state %s [to=%s]", from, from.getState(), to);

        // Handle simple cases
        switch (from.getState()) {
            case ERROR:
            case ON_HOLD:
                return DependencyStatus.HOLD;
            case DONE:
                break;
            default:
                return DependencyStatus.WAIT;
        }

        return _accept();
    }

    /**
     * Check the dependency status
     */
    abstract protected DependencyStatus _accept();

    /**
     * Lock the resource
     */
    protected abstract Lock _lock(String pid) throws LockException;


    /**
     * Update a dependency status
     *
     * @return True if the status changed
     */
    synchronized final public boolean update() {
        DependencyStatus old = status;
        status = accept();

        if (status == old)
            return false;

        return true;
    }

    final public Lock lock(String pid) throws LockException {
        lock = _lock(pid);
        return lock;
    }


    public void unactivate() {
        assert lock != null : "Lock of an active dependency is null";
        lock.close();
        lock = null;
        status = DependencyStatus.UNACTIVE;
    }
}