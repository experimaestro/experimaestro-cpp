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

import sf.net.experimaestro.exceptions.XPMRuntimeException;

import java.sql.SQLException;

/**
 * A reference to a resource
 */
public class ResourceReference {
    private Long id;

    private Resource resource;

    public ResourceReference(Resource resource) {
        this.resource = resource;
        this.id = resource.getId();
    }

    public ResourceReference(long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        if (resource != null) {
            return resource.toString();
        }
        return "R" + this.id;
    }

    public long id() {
        if (id != null) return id;
        return resource.getId();
    }

    Resource get() {
        if (resource == null) {
            try {
                resource = Resource.getById(id);
            } catch (SQLException e) {
                throw new XPMRuntimeException(e);
            }
        }
        return resource;
    }
}
