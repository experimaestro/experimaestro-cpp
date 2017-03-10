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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.AbstractIterator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory;
import com.google.gson.stream.JsonReader;
import net.bpiwowar.xpm.exceptions.CloseException;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.exceptions.XPMSQLException;
import net.bpiwowar.xpm.utils.CloseableIterable;
import net.bpiwowar.xpm.utils.ExceptionalConsumer;
import net.bpiwowar.xpm.utils.db.SQLInsert;
import net.bpiwowar.xpm.utils.log.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

import static java.lang.String.format;
import static net.bpiwowar.xpm.scheduler.Scheduler.prepareStatement;

/**
 * A set of objects stored in database
 */
final public class DatabaseObjects<T extends Identifiable, Information> {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * The name of the table
     */
    private final String tableName;

    private final String idFieldName;

    public interface Creator<T extends Identifiable, Information> {
        T create(DatabaseObjects<T, Information> db, ResultSet rs, Information information);
    }

    private final Creator<T, Information> create;

    /**
     * Keeps a cache of resources
     */
    final Cache<Long, T> map = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .softValues()
            .build();


    public DatabaseObjects(String tableName, Creator<T, Information> create) {
        this.tableName = tableName;
        this.idFieldName = "id";
        this.create = create;
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

        // 5 bits per character
        // A-Z, space, -, =,
        int shift = 58;
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
            shift -= 5;
        }

        return value;
    }

    public T findUnique(String query, ExceptionalConsumer<PreparedStatement> p) throws SQLException {
        return findUnique(query, p, null);
    }

    public T findUnique(String query, ExceptionalConsumer<PreparedStatement> p, Information information) throws SQLException {
        try (PreparedStatement st = Scheduler.getConnection().prepareCall(query)) {
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
                        return getOrCreate(result, information);
                    }
                }
            }

            // No result
            return null;
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    public CloseableIterable<T> find(final String query, final ExceptionalConsumer<PreparedStatement> p) throws SQLException {
        try {
            final CallableStatement st = Scheduler.getConnection().prepareCall(query);
            if (p != null) {
                p.apply(st);
            }
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

    public T getOrCreate(ResultSet result) throws SQLException {
        return getOrCreate(result, null);
    }

    public T getOrCreate(ResultSet result, Information information) throws SQLException {
        // Get from cache
        long id = result.getLong(1);
        T object = getFromCache(id);
        if (object != null) {
            return object;
        }

        // Create and cache
        final T t = create.create(this, result, information);
        synchronized (map) {
            map.put(t.getId(), t);
        }
        return t;
    }

    /**
     * Get an object from cache
     *
     * @param id The ID of the object
     * @return The object or null if none exists
     */
    public T getFromCache(long id) {
        synchronized (map) {
            return map.getIfPresent(id);
        }
    }


    final public void delete(T object) throws SQLException {
        synchronized (map) {
            try (final PreparedStatement st = Scheduler.getConnection().prepareStatement(format("DELETE FROM %s WHERE %s=?", tableName, idFieldName))) {
                st.setLong(1, object.getId());
                st.execute();
            }

            map.invalidate(object.getId());
            object.setId(null);
        }
    }

    public void save(T object, String query, ExceptionalConsumer<PreparedStatement> f) throws SQLException {
        save(object, query, f, false);
    }

    public void save(T object, String query, ExceptionalConsumer<PreparedStatement> f, boolean update)
            throws SQLException {
        try (PreparedStatement st = Scheduler.getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
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
        } catch (Exception e) {
            throw new XPMSQLException(e);
        }
    }

    public void save(T object, SQLInsert sqlInsert, boolean update, Object... values) throws SQLException {
        final long id = sqlInsert.execute(Scheduler.getConnection(), update, object.getId(), values);
        if (!update) {
            assert id >= 0;
            object.setId(id);
        }
        synchronized (map) {
            // We use a new key to avoid
            map.put(id, object);
        }
    }


    /**
     * Forget an ID - only used for testing
     */
    void forget(Long id) {
        synchronized (map) {
            map.invalidate(id);
        }
    }

    /**
     * Load data
     *
     * @param builder
     * @param object
     */
    public void loadData(GsonBuilder builder, T object, String dataFieldName) {
        try (PreparedStatement st = prepareStatement(format("SELECT %s FROM %s WHERE id=?", dataFieldName, tableName))) {
            st.setLong(1, object.getId());
            st.execute();
            final ResultSet resultSet = st.getResultSet();
            resultSet.next();
            final InputStream binaryStream = resultSet.getBinaryStream(1);
            loadFromJson(builder, object, binaryStream);
        } catch (SQLException e) {
            throw new XPMRuntimeException(e, "Could not retrieve data for %s", this);
        }
    }

    static public <T> void loadFromJson(GsonBuilder builder, T object, InputStream binaryStream) {
        final Gson gson = builder.create();
        try (InputStream is = binaryStream;
             Reader reader = new InputStreamReader(is);
             JsonReader jsonReader = new JsonReader(reader)
        ) {
            final ReflectiveTypeAdapterFactory.Adapter<T> adapter
                    = (ReflectiveTypeAdapterFactory.Adapter) gson.getAdapter(object.getClass());
            adapter.read(jsonReader, object);
        } catch (IOException e) {
            throw new XPMRuntimeException(e, "Error while deserializing JSON for [%s]", object);
        }
    }
}