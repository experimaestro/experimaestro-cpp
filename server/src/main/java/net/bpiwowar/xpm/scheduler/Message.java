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

/**
 * A message between resources
 */
abstract public class Message {
    private Event event;

    public Message(Event event) {
        this.event = event;
    }

    public Event getEvent() {
        return event;
    }

    public void send() {
        Scheduler.get().notify(this);
    }

    public enum Event {
        STORED_IN_DATABASE,
        STATE_CHANGED,
        END_OF_JOB,
        DEPENDENCY_CHANGED,
        PROGRESS,

        RESOURCE_ADDED,
        RESOURCE_REMOVED,

        EXPERIMENT_RESOURCE_ADDED, EXPERIMENT_ADDED
    }
}
