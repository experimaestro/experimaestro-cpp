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
    OK(1),

    /**
     * The resource can be used when properly locked
     */
    OK_LOCK(2),

    /**
     * The resource is not ready yet
     */
    WAIT(3),

    /**
     * The resource is not ready yet, and is on hold (this can only be changed
     * by the external intervention)
     */
    HOLD(4),

    /**
     * The resource is not ready, and this is due status an error (possibly among
     * dependencies)
     */
    ERROR(5),

    /**
     * Unactive dependency
     */
    UNACTIVE(6);

    private final int id;

    DependencyStatus(int id) {
        this.id = id;
    }

    /**
     * Returns true if the resource is ready
     * @return Boolean
     */
    public boolean isOK() {
        return this == OK_LOCK || this == OK;
    }

    /**
     * Returns whether this resource is in a blocking state
     * @return A boolean
     */
    public boolean isBlocking() {
        return this == HOLD || this == ERROR;
    }
}
