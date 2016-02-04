package net.bpiwowar.xpm.scheduler;

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

import net.bpiwowar.xpm.exceptions.LockException;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.locks.Lock;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.utils.GsonConverter;
import net.bpiwowar.xpm.utils.GsonSerialization;
import net.bpiwowar.xpm.utils.JsonSerializationInputStream;
import net.bpiwowar.xpm.utils.log.Logger;

import java.io.Serializable;
import java.sql.ResultSet;
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
            new Class[]{Long.TYPE, Long.TYPE, Lock.class, DependencyStatus.class}
    ).add(ExclusiveDependency.class, ReadWriteDependency.class, TokenDependency.class);

    public static final String _SELECT_DEPENDENCIES = "SELECT fromId, toId, type, status, lock, data FROM Dependencies";

    public static final String SELECT_OUTGOING_DEPENDENCIES = _SELECT_DEPENDENCIES + " WHERE fromId=?";

    public static final String SELECT_INGOING_DEPENDENCIES = _SELECT_DEPENDENCIES + " WHERE toId=?";

    public static final String SELECT_OUTGOING_ACTIVE_DEPENDENCIES = SELECT_OUTGOING_DEPENDENCIES + " AND status != " + DependencyStatus.UNACTIVE.getId();

    public static final String UPDATE_DEPENDENCY = "UPDATE Dependencies SET type=?, status=?, lock=?, data=? WHERE fromId=? and toId=?";

    public static final String INSERT_DEPENDENCY = "INSERT INTO Dependencies(type, status, lock, data, fromId, toId) VALUES(?,?,?,?,?,?)";

    final static private Logger LOGGER = Logger.getLogger();

    @GsonSerialization(serialize = false)
    ResourceReference from;

    @GsonSerialization(serialize = false)
    ResourceReference to;

    /**
     * The state of this dependency
     */
    @GsonSerialization(serialize = false)
    DependencyStatus status;

    /**
     * The lock (or null if no lock taken)
     */
    @GsonSerialization(serialize = false)
    private Lock lock;

    protected Dependency() {
    }

    /** Construct from database */
    protected Dependency(long fromId, long toId, Lock lock, DependencyStatus status) {
        this.from = new ResourceReference(fromId);
        this.to = new ResourceReference(toId);
        this.status = status;
        this.lock = lock;
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
        return to.equals(that.to);

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
        return status != old;

    }

    final public Lock lock(String pid) throws LockException {
        LOGGER.debug("Locking dependency %s", this);
        try {
            lock = _lock(pid);
            assert lock != null;

            Scheduler.statement("UPDATE Dependencies SET lock=? WHERE fromId=? and toId=?")
                    .setLong(1, lock.getId())
                    .setLong(2, from.id())
                    .setLong(3, to.id())
                    .execute().close();

            return lock;
        } catch (Throwable e) {
            throw new LockException(e);
        }
    }

    final public void unlock() throws LockException, SQLException {
        LOGGER.debug("Unlocking dependency %s", this);
        if (lock != null) {
            lock.close();
            lock = null;
        } else {
            LOGGER.warn("Lock of dependency %s is null", this);
        }

        // Update in DB
        Scheduler.statement("UPDATE Dependencies SET status=? WHERE fromId=? and toId=?")
                .setInt(1, status.ordinal())
                .setLong(2, from.id())
                .setLong(3, to.id())
                .execute().close();
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

    public void save(boolean update) throws SQLException {
        final XPMStatement st = Scheduler.statement(update ? UPDATE_DEPENDENCY : INSERT_DEPENDENCY)
                .setLong(1, DatabaseObjects.getTypeValue(this.getClass()))
                .setInt(2, status.getId())
                .setLong(3, lock == null ? null : lock.getId())
                .setBlob(4, JsonSerializationInputStream.of(this, GsonConverter.defaultBuilder))
                .setLong(5, from.id())
                .setLong(6, to.id());

        int updated =  st.executeUpdate();

                LOGGER.debug("Dependency %s - updated rows = %d", this, updated);
    }

    public long getToId() {
        return to.id();
    }

    /**
     * Create from a result. Must have been queried with {@linkplain #SELECT_INGOING_DEPENDENCIES},
     * {@linkplain #SELECT_OUTGOING_DEPENDENCIES}, or {@linkplain #SELECT_OUTGOING_ACTIVE_DEPENDENCIES}.
     *
     * @param rs
     * @return
     */
    public static Dependency create(ResultSet rs) throws SQLException {
        final long type = rs.getLong(3);
        final DependencyStatus dependencyStatus = DependencyStatus.fromId(rs.getShort(4));
        Long lockId = rs.getLong(5);
        Lock lock = rs.wasNull() ? null : Lock.findById(lockId);
        long fromId = rs.getLong(1);
        final Dependency dependency = REGISTRY.newInstance(type, fromId, rs.getLong(2), lock, dependencyStatus);
        DatabaseObjects.loadFromJson(GsonConverter.defaultBuilder, dependency, rs.getBinaryStream(6));
        return dependency;
    }

    public long getFromId() {
        return from.id();
    }
}