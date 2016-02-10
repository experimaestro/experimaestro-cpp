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
 * Simple messages that can be sent status resources
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class ResourceMessage extends Message {
    /** The resource ID */
    private Long id;

    String locator;
    double progress;
    ResourceState state;

    private ResourceMessage(Event event, Resource resource) {
        super(event);
        this.id = resource.getId();
        this.locator = resource.getLocator().toString();
        this.progress = resource instanceof Job ? ((Job) resource).getProgress() : 0;
        this.state = resource.getState();
    }

    public static ResourceMessage added(Resource resource) {
        return new ResourceMessage(Event.RESOURCE_ADDED, resource);
    }

    public static Message changed(Resource resource) {
        return new ResourceMessage(Event.STATE_CHANGED, resource);
    }

    public static ResourceMessage removed(Resource resource) {
        return new ResourceMessage(Event.RESOURCE_REMOVED, resource);
    }
}
