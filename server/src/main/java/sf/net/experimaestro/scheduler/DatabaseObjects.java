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

import com.google.common.collect.AbstractIterator;
import sf.net.experimaestro.exceptions.CloseException;
import sf.net.experimaestro.exceptions.DatabaseException;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.utils.CloseableIterable;
import sf.net.experimaestro.utils.Functional;
import sf.net.experimaestro.utils.log.Logger;

import java.sql.*;
import java.util.Iterator;
import java.util.WeakHashMap;

import static java.lang.String.format;

/**
 * A set of objects stored in database
 */
public abstract class DatabaseObjects<T extends Identifiable> {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * The underlying SQL connection
     */
    protected final Connection connection;

    private final String tableName;

    private final String idFieldName;

    /**
     * Keeps a cache of resources
     */
    private WeakHashMap<Long, T> map = new WeakHashMap<>();


    public DatabaseObjects(Connection connection) {
        this.connection = connection;
        this.tableName = this.getClass().getSimpleName();
        this.idFieldName = "id";
    }


    public static long getTypeValue(Class<?> aClass) {
        return getTypeValue(aClass.getAnnotation(TypeIdentifier.class).value());
    }

    /**
     * @param typeString
     * @return
     */
    public static long getTypeValue(String typeString) {
        if (typeString.length() > 64 / 5) {
            throw new XPMRuntimeException("Type %s cannot be converted to value (too long - limit 12 chars)", typeString);
        }

        // A-Z, space, -, =,
        int shift = 24;
        long value = 0;
        for (int i = 0; i < typeString.length(); ++i) {
            final char c = typeString.charAt(i);
            final int v;
            if (c >= 'A' && c <= 'Z') {
                v = Character.getNumericValue(c) - Character.getNumericValue('A') + 6;
            } else if (c >= 'a' && c <= 'z') {
                v = Character.getNumericValue(c) - Character.getNumericValue('a') + 6;
            } else {
                switch (c) {
                    case ' ':
                        v = 0;
                        break;
                    case '-':
                        v = 1;
                        break;
                    case ':':
                        v = 2;
                        break;
                    case '.':
                        v = 3;
                        break;
                    case '@':
                        v = 4;
                        break;
                    case '_':
                        v = 5;
                        break;
                    default:
                        throw new XPMRuntimeException("Type %s cannot be converted to value", typeString);
                }
            }
            value += (long) (v << shift);
        }

        return value;
    }

    protected T findUnique(String query, Functional.ExceptionalConsumer<PreparedStatement> p) throws DatabaseException {
        try (PreparedStatement st = connection.prepareCall(query)) {
            // Sets the variables
            p.apply(st);

            // Execute and get the object
            if (st.execute()) {
                try (ResultSet result = st.getResultSet()) {
                    if (result.next()) {
                        // Get from cache
                        long id = result.getLong(1);
                        T object = getFromCache(id);
                        if (object != null) {
                            return object;
                        }

                        // Construct
                        return getOrCreate(result);
                    }
                }
            }

            // No result
            return null;
        } catch (Throwable e) {
            throw new DatabaseException(e);
        }
    }

    CloseableIterable<T> find(final String query, final Functional.ExceptionalConsumer<PreparedStatement> p) throws DatabaseException {
        try {
            final CallableStatement st = connection.prepareCall(query);
            p.apply(st);
            LOGGER.debug("Executing query: %s", st);

            final ResultSet result = st.executeQuery();

            return new CloseableIterable<T>() {
                @Override
                public void close() throws CloseException {
                    try {
                        st.close();
                        result.close();
                    } catch (SQLException e) {
                        throw new CloseException(e);
                    }
                }

                @Override
                public Iterator<T> iterator() {
                    return new AbstractIterator<T>() {
                        @Override
                        protected T computeNext() {
                            try {
                                if (!result.next()) {
                                    return endOfData();
                                }
                                return getOrCreate(result);


                            } catch (SQLException | DatabaseException e) {
                                throw new XPMRuntimeException(e, "Error while fetching resource from DB");
                            }
                        }
                    };
                }
            };
        } catch (Exception e) {
            throw new DatabaseException(e);
        }
    }

    private T getOrCreate(ResultSet result) throws SQLException, DatabaseException {
        // Get from cache
        long id = result.getLong(1);
        T object = getFromCache(id);
        if (object != null) {
            return object;
        }

        // Create and cache
        final T t = create(result);
        synchronized (map) {
            map.put(t.getId(), t);
        }
        return t;
    }

    /**
     * Create a new object from a result
     *
     * @param result The result
     * @return The new object
     */
    abstract protected T create(ResultSet result) throws DatabaseException;

    /**
     * Get an object from cache
     *
     * @param id The ID of the object
     * @return The object or null if none exists
     */
    protected T getFromCache(long id) {
        synchronized (map) {
            return map.get(id);
        }
    }


    final public void delete(T object) throws DatabaseException {
        synchronized (map) {
            try {
                try (final PreparedStatement st = connection.prepareStatement(format("DELETE FROM %s WHERE %s=?", tableName, idFieldName))) {
                    st.setLong(1, object.getId());
                    st.execute();
                }
            } catch (SQLException e) {
                throw new DatabaseException(e);
            }

            final T remove = map.remove(object.getId());
            assert remove != null : "Object was not in cache !";

            remove.setId(null);
        }
    }

    protected void save(T object, String query, Functional.ExceptionalConsumer<PreparedStatement> f)
            throws DatabaseException {
        try (PreparedStatement st = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            // Get the key
            f.apply(st);
            int affectedRows = st.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("Creating object failed, no rows inserted.");
            }
            try (ResultSet generatedKeys = st.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    object.setId(generatedKeys.getLong(1));
                } else {
                    throw new DatabaseException("Creating object failed, no ID obtained.");
                }
            }

            try(final PreparedStatement statement = connection.prepareStatement("SELECT path, status FROM Resources")) {
                final ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    LOGGER.info("Row: %s in state %d", resultSet.getString(1), resultSet.getLong(2));
                }
            }


            synchronized (map) {
                map.put(object.getId(), object);
            }
        } catch (Exception e) {
            throw new DatabaseException(e);
        }
    }


}
