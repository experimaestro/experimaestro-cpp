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
import com.google.gson.Gson;
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory;
import com.google.gson.stream.JsonReader;
import sf.net.experimaestro.exceptions.CloseException;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.utils.CloseableIterable;
import sf.net.experimaestro.utils.ExceptionalConsumer;
import sf.net.experimaestro.utils.GsonConverter;
import sf.net.experimaestro.utils.db.SQLInsert;
import sf.net.experimaestro.utils.log.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.WeakHashMap;

import static java.lang.String.format;
import static sf.net.experimaestro.scheduler.Scheduler.prepareStatement;

/**
 * A set of objects stored in database
 */
public abstract class DatabaseObjects<T extends Identifiable> {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * The underlying SQL connection
     */
    protected final Connection connection;

    /**
     * The name of the table
     */
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
        final TypeIdentifier annotation = aClass.getAnnotation(TypeIdentifier.class);
        assert annotation != null : format("Class %s has no TypeIdentifier annotation", aClass);

        return getTypeValue(annotation.value());
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

    protected T findUnique(String query, ExceptionalConsumer<PreparedStatement> p) throws SQLException {
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
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    CloseableIterable<T> find(final String query, final ExceptionalConsumer<PreparedStatement> p) throws SQLException {
        try {
            {
                final CallableStatement st = connection.prepareCall(query);
                p.apply(st);
                LOGGER.debug("Executing query: %s", st);

                final ResultSet result = st.executeQuery();
                int i = 0;
                while (result.next()) {
                    ++i;
                    final T t = getOrCreate(result);
                    LOGGER.debug("Result %d: %s", i, t);
                }
            }

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


                            } catch (SQLException e) {
                                throw new XPMRuntimeException(e, "Error while fetching resource from DB");
                            }
                        }
                    };
                }
            };
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    private T getOrCreate(ResultSet result) throws SQLException {
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
    abstract protected T create(ResultSet result) throws SQLException;

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


    final public void delete(T object) throws SQLException {
        synchronized (map) {
            try (final PreparedStatement st = connection.prepareStatement(format("DELETE FROM %s WHERE %s=?", tableName, idFieldName))) {
                st.setLong(1, object.getId());
                st.execute();
            }


            final T remove = map.remove(object.getId());
            assert remove != null : "Object was not in cache !";

            remove.setId(null);
        }
    }

    protected void save(T object, String query, ExceptionalConsumer<PreparedStatement> f) throws SQLException {
        save(object, query, f, false);
    }

    public void save(T object, String query, ExceptionalConsumer<PreparedStatement> f, boolean update)
            throws SQLException {
        try (PreparedStatement st = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            // Get the key
            f.apply(st);
            int affectedRows = st.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating object failed, no rows inserted.");
            }

            if (!update) {
                try (ResultSet generatedKeys = st.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        object.setId(generatedKeys.getLong(1));
                    } else {
                        throw new SQLException("Creating object failed, no ID obtained.");
                    }
                }
            }


            synchronized (map) {
                map.put(object.getId(), object);
            }
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    public void save(T object, SQLInsert sqlInsert, boolean update, Object... values) throws SQLException {
        final long id = sqlInsert.execute(connection, update, object.getId(), values);
        if (!update) {
            assert id >= 0;
            object.setId(id);
        }
        synchronized (map) {
            map.put(object.getId(), object);
        }
    }



    /**
     * Forget an ID - only used for testing
     */
    void forget(Long id) {
        synchronized (map) {
            map.remove(id);
        }
    }

    /**
     * Load data
     *
     * @param object
     */
    public void loadData(T object, String dataFieldName) {
        final Gson gson = GsonConverter.builder.create();
        try (PreparedStatement st = prepareStatement(format("SELECT %s FROM %s WHERE id=?", dataFieldName, tableName))) {
            st.setLong(1, object.getId());
            st.execute();
            final ResultSet resultSet = st.getResultSet();
            resultSet.next();
            try (InputStream is = resultSet.getBinaryStream(1);
                 Reader reader = new InputStreamReader(is);
                 JsonReader jsonReader = new JsonReader(reader)
            ) {
                final ReflectiveTypeAdapterFactory.Adapter<T> adapter
                        = (ReflectiveTypeAdapterFactory.Adapter) gson.getAdapter(object.getClass());
                adapter.read(jsonReader, object);
            } catch (IOException e) {
                throw new XPMRuntimeException(e, "Error while deserializing JSON for [%s]", this);
            }
        } catch (SQLException e) {
            throw new XPMRuntimeException(e, "Could not retrieve data for %s", this);
        }
    }

}