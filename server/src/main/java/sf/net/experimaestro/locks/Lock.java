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
import sf.net.experimaestro.scheduler.DatabaseObjects;
import sf.net.experimaestro.scheduler.Identifiable;
import sf.net.experimaestro.scheduler.Locks;
import sf.net.experimaestro.scheduler.Resources;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.JsonSerializationInputStream;
import sf.net.experimaestro.utils.db.SQLInsert;

import java.sql.PreparedStatement;
import java.sql.SQLException;

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
    final public void close() throws LockException, SQLException {
        // Do close
        doClose();

        // Remove from DB
        if (inDatabase()) {
            Scheduler.statement("DELETE FROM Locks WHERE id=?").setLong(1, id).execute();
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

    SQLInsert sqlInsert = new SQLInsert("Locks", true, "id", "type", "data");

    protected void save(Locks locks) throws SQLException {
        locks.save(this, sqlInsert, false,
                DatabaseObjects.getTypeValue(this.getClass()), JsonSerializationInputStream.of(this));
    }


}

