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

/**
 * Defines how a given lock type is satisfied by the current state
 * of the resource
 */
public enum DependencyStatus {
    /**
     * The resource can be used as is
     */
    OK,

    /**
     * The resource can be used when properly locked
     */
    OK_LOCK,

    /**
     * The resource is not ready yet
     */
    WAIT,

    /**
     * The resource is not ready yet, and is on hold (this can only be changed
     * by the external intervention)
     */
    HOLD,

    /**
     * The resource is not ready, and this is due status an error (possibly among
     * dependencies)
     */
    ERROR,

    /**
     * Unactive dependency
     */
    UNACTIVE;

    private static final DependencyStatus VALUES[] = new DependencyStatus[]{OK, OK_LOCK, WAIT, HOLD, ERROR, UNACTIVE};

    static {
        short id = 0;
        for (DependencyStatus value : VALUES) {
            value.id = id++;
        }

    }

    private short id;

    DependencyStatus() {
    }

    /**
     * Returns true if the resource is ready
     *
     * @return Boolean
     */
    public boolean isOK() {
        return this == OK_LOCK || this == OK;
    }

    /**
     * Returns whether this resource is in a blocking state
     *
     * @return A boolean
     */
    public boolean isBlocking() {
        return this == HOLD || this == ERROR;
    }

    public short getId() {
        return id;
    }

    public static DependencyStatus fromId(int id) {
        if (id < 0 || id >= VALUES.length) {
            throw new IndexOutOfBoundsException("id " + id + " is not valid as a dependency status");
        }
        return VALUES[id];
    }
}
