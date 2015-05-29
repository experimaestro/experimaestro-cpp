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

import sf.net.experimaestro.exceptions.DatabaseException;
import sf.net.experimaestro.utils.CloseableIterable;
import sf.net.experimaestro.utils.CloseableIterator;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;

/**
 * Access to all resources
 */
public class Resources extends DatabaseObjects<Resource> {
    /**
     * The default query
     */
    public static final String SELECT_BEGIN = "SELECT id, type, path, status FROM resources";

    static private ConstructorRegistry<Resource> REGISTRY = new ConstructorRegistry(new Class[]{})
            .add(Resource.class, CommandLineTask.class, TokenResource.class);

    /**
     * Initialize resources
     */
    public Resources(Connection connection) {
        super(connection);
    }

    /**
     * Get a resource by locator
     *
     * @param path The path of the resource
     * @return The resource or null if there is no such resource
     */
    public Resource getByLocator(String path) throws DatabaseException {
        return findUnique(SELECT_BEGIN + " WHERE path=?", st -> st.setString(1, path));
    }

    @Override
    protected Resource create(ResultSet result) throws DatabaseException {
        try {
            long id = result.getLong(1);
            long type = result.getLong(2);
            String path = result.getString(3);
            long status = result.getLong(4);

            final Constructor<? extends Resource> constructor = REGISTRY.get(type);
            final Resource resource = constructor.newInstance(id, path);

            // Set stored values
            resource.setState(ResourceState.fromValue(status));

            return resource;
        } catch (SQLException | InstantiationException | InvocationTargetException | IllegalAccessException e) {
            throw new DatabaseException(e, "Error retrieving database object");
        }
    }

    /**
     * Iterator on resources
     */
    public CloseableIterable<Resource> resources() throws DatabaseException {
        return find(SELECT_BEGIN, st -> {});
    }

    public CloseableIterator<Resource> get(EnumSet<ResourceState> states) {
        return null;
    }

    public Resource getById(long resourceId) throws DatabaseException {
        final Resource r = getFromCache(resourceId);
        if (r != null) {
            return r;
        }

        return findUnique(SELECT_BEGIN + " WHERE id=?", st -> st.setLong(1, resourceId));
    }
}
