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

import org.apache.commons.lang.NotImplementedException;

import java.util.EnumSet;
import java.util.HashMap;

/**
 * The resource state
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public enum ResourceState {
    /**
     * For a job only: the job is waiting dependencies status be met
     */
    WAITING,

    /**
     * For a job only: the job is waiting for an available thread status launch it
     */
    READY,

    /**
     * For a job only: The job is currently running
     */
    RUNNING,

    /**
     * The job is on hold
     */
    ON_HOLD,

    /**
     * The job ran but did not complete or the data was not generated
     */
    ERROR,

    /**
     * Completed (for a job) or generated (for a data resource)
     */
    DONE;

    static private final HashMap<Long, ResourceState> REGISTRY = new HashMap<>();
    static {
        for(ResourceState state: ResourceState.values()) {
            REGISTRY.put(state.value, state);
        }
    }

    /**
     * States in which a resource can replaced
     */
    final static EnumSet<ResourceState> UPDATABLE_STATES
            = EnumSet.of(READY, ON_HOLD, ERROR, WAITING);

    /**
     * States in which a resource can be notified
     */
    final static EnumSet<ResourceState> NOTIFIABLE_STATE
            = EnumSet.complementOf(EnumSet.of(RUNNING, DONE, ERROR));


    /**
     * States in which the job is not active
     */
    final static EnumSet<ResourceState> UNACTIVE_STATE
            = EnumSet.of(DONE, ERROR, ON_HOLD);

    /**
     * States in which the job has finished (whatever the outcome)
     */
    final static EnumSet<ResourceState> FINISHED_STATE
            = EnumSet.of(DONE, ERROR, ON_HOLD);

    /**
     * Database value
     */
    private final long value;


    /**
     * Returns true if the resource is not done and
     * is blocked (error or hold)
     *
     * @return
     */
    public boolean isBlocking() {
        return this == ON_HOLD || this == ERROR;
    }

    /**
     * Returns whether a job is active (waiting, ready or running)
     *
     * @return
     */
    public boolean isActive() {
        return this == WAITING || this == RUNNING || this == READY;
    }


    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

    public boolean isUnactive() {
        return UNACTIVE_STATE.contains(this);
    }

    public boolean isFinished() {
        return FINISHED_STATE.contains(this);
    }

    public static ResourceState fromValue(long status) {
        return REGISTRY.get(status);
    }

    ResourceState() {
        value = DatabaseObjects.getTypeValue(toString());
    }
    public long value() {
        return value;
    }
}
