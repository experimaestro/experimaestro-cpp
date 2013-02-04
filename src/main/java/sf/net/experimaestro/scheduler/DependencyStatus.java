/*
 * This file is part of experimaestro.
 * Copyright (c) 2013 B. Piwowarski <benjamin@bpiwowar.net>
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
     * The resource is not ready, and this is due to an error (possibly among
     * dependencies)
     */
    ERROR, UNACTIVE;

    public boolean isOK() {
        return this == OK_LOCK || this == OK;
    }

    public boolean isBlocking() {
        return this == HOLD || this == ERROR;
    }
}
