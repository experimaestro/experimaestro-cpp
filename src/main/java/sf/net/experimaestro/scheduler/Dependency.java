/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.scheduler;

import com.sleepycat.persist.model.*;
import sf.net.experimaestro.exceptions.LockException;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.utils.log.Logger;

/**
 * What is the state of a dependency.
 * This class stores the previous state
 * (satisfied or not) in order to updateFromStatusFile the number of blocking resources
 */
@Entity
abstract public class Dependency {
    final static private Logger LOGGER = Logger.getLogger();

    public static final String FROM_KEY_NAME = "from";
    public static final String TO_KEY_NAME = "to";

    @PrimaryKey(sequence = "dependency_id")
    private long id;

    /**
     * The resource.
     * We abort its deletion if there is a dependency.
     */
    @SecondaryKey(name = FROM_KEY_NAME, relate = Relationship.MANY_TO_ONE, onRelatedEntityDelete = DeleteAction.ABORT)
    private long from;

    /**
     * The resource that depends on the resource {@link #from}
     */
    @SecondaryKey(name = TO_KEY_NAME, relate = Relationship.MANY_TO_ONE, relatedEntity = Resource.class, onRelatedEntityDelete = DeleteAction.CASCADE)
    private long to;

    /**
     * The state of this dependency
     */
    DependencyStatus status;

    /**
     * The lock (or null if no lock taken)
     */
    private Lock lock;


    protected Dependency() {
    }

    public Dependency(long from) {
        this.from = from;
    }

    public Dependency setTo(long to) {
        this.to = to;
        return this;
    }


    public long getFrom() {
        return from;
    }

    public long getTo() {
        return to;
    }

    @Override
    public String toString() {
        return String.format("Dep[R%d-R%d]; %s", from, to, status);
    }

    /**
     * Can the dependency be accepted?
     *
     * @param scheduler The scheduler to retrieve data from database
     * @param from The requirement (to avoid a fetch from database) or null
     *
     * @return {@link DependencyStatus#OK} if the dependency is satisfied,
     *         {@link DependencyStatus#WAIT} if it can be satisfied one day
     *         {@link DependencyStatus#HOLD} if it can be satisfied after an external change
     *         {@link DependencyStatus#ERROR} if it cannot be satisfied
     */
    protected DependencyStatus accept(Scheduler scheduler, Resource from) {
        if (from == null) {
            from = scheduler.getResource(this.from);
            if (from == null)
                throw new AssertionError(String.format("Could not find resource with ID %d", this.from));
        }

        assert from.getId() == this.from;

        LOGGER.debug("From [%d] is in state %s [to=%s]", this.from, from.getState(), to);

        // Handle simple cases
        if (from.getState() == ResourceState.ERROR)
            return DependencyStatus.HOLD;

        if (from.getState() == ResourceState.ON_HOLD)
            return DependencyStatus.HOLD;

        // If not done, then we wait
        if (from.getState() != ResourceState.DONE)
            return DependencyStatus.WAIT;

        return _accept(scheduler, from);
    }

    /**
     * Check the dependency status
     */
    abstract protected DependencyStatus _accept(Scheduler scheduler, Resource from);

    /**
     * Lock the resource
     */
    protected abstract Lock _lock(Scheduler scheduler, Resource from, String pid) throws LockException;


    /**
     * Update a dependency status
     *
     * @param scheduler
     * @param from      The  resource to be locked, i.e. {@linkplain #from} or null (in this case,
     *                  it might be loaded from DB using the scheduler)
     * @param store     <tt>true</tt> if the dependency status should be stored in DB if changed.
     * @return
     */
    synchronized final public boolean update(Scheduler scheduler, Resource from, boolean store) {
        DependencyStatus old = status;
        status = accept(scheduler, from);

        if (status == old)
            return false;

        if (store)
            scheduler.store(this);
        return true;
    }

    final public Lock lock(Scheduler scheduler, Resource from, String pid) throws LockException {
        lock = _lock(scheduler, from, pid);
        if (lock != null) {
            scheduler.store(this);
        }
        return lock;
    }



    protected Resource getFrom(Scheduler scheduler, Resource from) {
        return from != null ? from : scheduler.getResource(getFrom());
    }


}