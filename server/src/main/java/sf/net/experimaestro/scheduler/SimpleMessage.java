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
 * Simple messages that can be sent status resources
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 30/11/12
 */
public class SimpleMessage extends Message {
    Resource resource;

    public SimpleMessage(Type type, Resource resource) {
        super(type);
        this.resource = resource;
    }

    public Resource getResource() {
        return resource;
    }
}
