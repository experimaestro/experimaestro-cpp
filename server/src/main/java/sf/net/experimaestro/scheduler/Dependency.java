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
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.manager.scripting.Exposed;
import sf.net.experimaestro.utils.log.Logger;

import java.io.Serializable;
import java.sql.SQLException;

import static java.lang.String.format;

/**
 * What is the state of a dependency.
 * This class stores the previous state
 * (satisfied or not) in order status updateFromStatusFile the number of blocking resources
 */
@Exposed
abstract public class Dependency implements Serializable {
    final static private ConstructorRegistry<Dependency> REGISTRY = new ConstructorRegistry(
            new Class[]{Long.TYPE, Long.TYPE, DependencyStatus.class}
    ).add(ExclusiveDependency.class, ReadWriteDependency.class, TokenDependency.class);

    public static final String UPDATE_DEPENDENCY = "UPDATE Dependencies SET type=? and status=? WHERE fromId=? and toId=?";
    public static final String INSERT_DEPENDENCY = "INSERT INTO Dependencies(type, status, fromId, toId) VALUES(?,?,?,?)";

    final static private Logger LOGGER = Logger.getLogger();

    ResourceReference from;

    ResourceReference to;

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

    protected Dependency(long fromId, long toId, DependencyStatus status) {
        this.from = new ResourceReference(fromId);
        this.to = new ResourceReference(toId);
        this.status = status;
    }

    public Dependency(Resource from) {
        this.from = new ResourceReference(from);
    }

    protected Dependency(Resource from, Resource to) {
        this.from = from.reference();
        this.to = to.reference();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dependency)) return false;

        Dependency that = (Dependency) o;

        if (!from.equals(that.from)) return false;
        if (!to.equals(that.to)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = from.hashCode();
        result = 31 * result + to.hashCode();
        return result;
    }


    public boolean hasLock() {
        return lock != null;
    }

    public Resource getFrom() {
        return from.get();
    }

    public Resource getTo() {
        return to.get();
    }

    @Override
    public String toString() {
        return format("Dep[%s-%s]; %s; %b", from, to, status, hasLock());
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
        LOGGER.debug("From [%s] is in state %s [status=%s]", from, getFrom().getState(), to);

        // Handle simple cases
        switch (getFrom().getState()) {
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
        LOGGER.debug("Locking dependency %s", this);
        try {
            lock = _lock(pid);
            assert lock != null;
            return lock;
        } catch (Throwable e) {
            throw new LockException(e);
        }
    }

    final public void unlock() throws LockException, SQLException {
        LOGGER.debug("Unlocking dependency %s", this);
        assert lock != null : format("Lock of dependency %s is null", this);
        lock.close();
        lock = null;
        status = DependencyStatus.UNACTIVE;
    }

    public void replaceBy(Dependency dependency) {
        assert dependency.getFrom().getId() == this.getFrom().getId();
        assert dependency.getTo().getId() == this.getTo().getId();
        if (dependency.getClass() != dependency.getClass())
            throw new AssertionError("Cannot replace dependency");

        doReplaceBy(dependency);
        try {
            save(true);
        } catch (SQLException e) {
            throw new XPMRuntimeException(e, "Could not update in Db");
        }
    }

    void doReplaceBy(Dependency dependency) {
        // Do nothing ATM
    }

    public static Dependency create(long fromId, long toId, long type, DependencyStatus dependencyStatus) {
        return REGISTRY.newInstance(type, fromId, toId, dependencyStatus);
    }

    public void save(boolean update) throws SQLException {
        Scheduler.statement(update ? UPDATE_DEPENDENCY : INSERT_DEPENDENCY)
                .setLong(1, DatabaseObjects.getTypeValue(this.getClass()))
                .setInt(2, status.getId())
                .setLong(3, from.id())
                .setLong(4, to.id())
                .execute();
    }
}