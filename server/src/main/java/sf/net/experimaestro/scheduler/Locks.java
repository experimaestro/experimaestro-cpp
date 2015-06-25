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

import sf.net.experimaestro.locks.Lock;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import static java.lang.String.format;

/**
 * Access to connectors
 */
public class Locks extends DatabaseObjects<Lock> {
    public static final String SELECT_QUERY = "SELECT id, type, uri, value FROM Locks";

    /**
     * Our registry
     */
    static private ConstructorRegistry<Lock> REGISTRY
            = new ConstructorRegistry(new Class[]{}).add();

    public Locks(Connection connection) {
        super(connection);
    }


    public Lock find(String uri) throws SQLException {
        final String query = format("%s WHERE path = ?", SELECT_QUERY);
        return findUnique(query, st -> st.setString(1, uri));
    }

    @Override
    protected Lock create(ResultSet result) throws SQLException {
        try {
            // OK, create connector
            long id = result.getLong(1);
            long type = result.getLong(2);
            String uri = result.getString(3);
            String value = result.getString(4);

            final Constructor<? extends Lock> constructor = REGISTRY.get(type);
            final Lock connector = constructor.newInstance(id, uri, value);

            return connector;
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
            throw new SQLException("Error retrieving database object", e);
        }
    }

}
