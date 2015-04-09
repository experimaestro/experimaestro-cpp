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

import javax.persistence.EntityManager;

/**
 * A message between resources
 */
abstract public class Message {
    private Type type;

    public Message(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        STORED_IN_DATABASE,
        STATE_CHANGED,
        END_OF_JOB,
        DEPENDENCY_CHANGED,

        RESOURCE_ADDED,
        RESOURCE_REMOVED;
    }
}
