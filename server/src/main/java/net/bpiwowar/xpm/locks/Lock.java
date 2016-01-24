package net.bpiwowar.xpm.locks;

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

import net.bpiwowar.xpm.connectors.NetworkShare;
import net.bpiwowar.xpm.exceptions.LockException;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.fs.XPMPath;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.scheduler.ConstructorRegistry;
import net.bpiwowar.xpm.scheduler.DatabaseObjects;
import net.bpiwowar.xpm.scheduler.Identifiable;
import net.bpiwowar.xpm.scheduler.Scheduler;
import net.bpiwowar.xpm.scheduler.StatusLock;
import net.bpiwowar.xpm.scheduler.TokenLock;
import net.bpiwowar.xpm.utils.GsonConverter;
import net.bpiwowar.xpm.utils.JsonAbstract;
import net.bpiwowar.xpm.utils.JsonSerializationInputStream;
import net.bpiwowar.xpm.utils.db.SQLInsert;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;

import static java.lang.String.format;

/**
 * A lock that can be removed.
 * <p/>
 * The lock is taken during the object construction which is dependent on the
 * actual {@link Lock} implementation.
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Exposed
@JsonAbstract
public abstract class Lock implements AutoCloseable, Identifiable {
    static protected ConstructorRegistry<Lock> REGISTRY
            = new ConstructorRegistry(new Class[]{ Long.TYPE }).add(TokenLock.class, FileLock.class, StatusLock.class);

    private Long id;
    public static final String SELECT_QUERY = "SELECT id, type, data FROM Locks";
    public static final String PROCESS_SELECT_QUERY = "SELECT id, type, data FROM Locks, ProcessLocks WHERE id = lock AND process=?";

    public Lock() {}

    public Lock(long id) {
        this.id = id;
    }

    @Override
    final public void close() throws LockException, SQLException {
        // Do close
        doClose();

        // Remove from DB
        if (inDatabase()) {
            Scheduler.statement("DELETE FROM Locks WHERE id=?").setLong(1, id)
                    .execute().close();
        }
    }

    protected abstract void doClose() throws LockException;

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

    synchronized final public void save() throws SQLException {
        save(Scheduler.get().locks());
    }

    static private final SQLInsert sqlInsert = new SQLInsert("Locks", true, "id", "type", "data");

    protected void save(DatabaseObjects<Lock> locks) throws SQLException {
        locks.save(this, sqlInsert, false,
                DatabaseObjects.getTypeValue(this.getClass()), JsonSerializationInputStream.of(this, GsonConverter.defaultBuilder));
    }

    protected void saveShare(Path path) throws SQLException {
        // Add the share into DB so that we keep the reference
        if (path instanceof XPMPath) {
            final XPMPath xpmPath = (XPMPath) path;
            final NetworkShare networkShare = NetworkShare.find(xpmPath.getHostName(), xpmPath.getShareName());
            Scheduler.statement("INSERT INTO LockShares(lock, share) VALUES(?,?)")
                    .setLong(1, getId())
                    .setLong(2, networkShare.getId())
                    .execute().close();
        }
    }

    public static Lock findById(long id) throws SQLException {
        return Scheduler.get().locks().findUnique(SELECT_QUERY + " WHERE id=?", st -> st.setLong(1, id));
    }

    public static Lock create(DatabaseObjects<Lock> db, ResultSet rs) {
        try {
            long id = rs.getLong(1);
            final Lock lock = REGISTRY.get(rs.getLong(2)).newInstance(id);
            DatabaseObjects.loadFromJson(GsonConverter.defaultBuilder, lock, rs.getBinaryStream(3));
            return lock;
        } catch(Throwable e) {
            throw new XPMRuntimeException(e, "Could not create lock object from DB");
        }
    }

}

