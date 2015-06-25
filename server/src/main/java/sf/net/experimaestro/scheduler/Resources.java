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

import com.google.gson.stream.JsonWriter;
import sf.net.experimaestro.utils.CloseableIterable;
import sf.net.experimaestro.utils.log.Logger;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.Iterator;

import static sf.net.experimaestro.utils.Functional.runnable;

/**
 * Access to all resources
 */
public class Resources extends DatabaseObjects<Resource> {

    final static private Logger LOGGER = Logger.getLogger();

    /**
     * The default query
     */
    public static final String SELECT_BEGIN = "SELECT id, type, path, status FROM resources";

    public static final String INSERT_DEPENDENCY = "INSERT INTO Dependencies(fromId, toId, status) VALUES(?,?,?)";

    static ConstructorRegistry<Resource> REGISTRY = new ConstructorRegistry(new Class[]{Long.class, String.class})
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
    public Resource getByLocator(String path) throws SQLException {
        return findUnique(SELECT_BEGIN + " WHERE path=?", st -> st.setString(1, path));
    }

    @Override
    protected Resource create(ResultSet result) throws SQLException {
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
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
            throw new SQLException("Error retrieving database object", e);
        }
    }

    /**
     * Iterator on resources
     */
    public CloseableIterable<Resource> resources() throws SQLException {
        return find(SELECT_BEGIN, st -> {
        });
    }

    static public class Placeholders implements Iterable<String> {
        int count;

        public Placeholders(int count) {
            this.count = count;
        }

        @Override
        public Iterator<String> iterator() {
            return new Iterator<String>() {
                int i = 0;

                @Override
                public boolean hasNext() {
                    return i < count;
                }

                @Override
                public String next() {
                    ++i;
                    return "?";
                }
            };
        }
    }

    public CloseableIterable<Resource> find(EnumSet<ResourceState> states) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append(SELECT_BEGIN);
        sb.append(" WHERE status in (");
        boolean first = true;
        for (ResourceState state : states) {
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }
            sb.append(getTypeValue(state.toString()));
        }
        sb.append(")");

        final String query = sb.toString();
        LOGGER.debug("Searching for resources in states %s: %s", states, query);
        return find(query, st -> {
        });
    }

    public Resource getById(long resourceId) throws SQLException {
        final Resource r = getFromCache(resourceId);
        if (r != null) {
            return r;
        }

        return findUnique(SELECT_BEGIN + " WHERE id=?", st -> st.setLong(1, resourceId));
    }

    public void save(Resource resource) throws SQLException {
        if (resource.getId() != null) {
            throw new SQLException("Resource already in database");
        }

        // Save resource
        // Save on file
        try (final PipedOutputStream os = new PipedOutputStream()) {
            final PipedInputStream is = new PipedInputStream(os);

            LOGGER.debug("Saving resource [%s] of type %s [%d], status %s [%s]",
                    resource.getLocator(), resource.getClass(), getTypeValue(resource.getClass()),
                    resource.getState(), resource.getState().value());
            save(resource, "INSERT INTO Resources(type, path, status, data) VALUES(?, ?, ?, ?)", st -> {
                st.setLong(1, getTypeValue(resource.getClass()));
                st.setString(2, resource.getLocator());
                st.setLong(3, resource.getState().value());
                new Thread(runnable(() -> {
                    try (final OutputStreamWriter out = new OutputStreamWriter(os);
                         JsonWriter writer = new JsonWriter(out)) {
                        resource.saveJson(writer);
                    }
                }), "JsonWriter").start();
                st.setBlob(4, is);
            });
        } catch (IOException e) {
            throw new SQLException(e);
        }

        // Save dependencies
        try (PreparedStatement st = connection.prepareStatement(INSERT_DEPENDENCY)) {
            st.setLong(2, resource.getId());
            for (Dependency dependency : resource.getIngoingDependencies().values()) {
                st.setLong(1, dependency.getFrom().getId());
                st.setLong(3, dependency.status.getId());
                st.execute();
            }
        }

    }
}
