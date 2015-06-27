package sf.net.experimaestro.utils.db;

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


import sf.net.experimaestro.exceptions.XPMIllegalArgumentException;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A class to ease inserts into database
 */
public class SQLInsert {
    final int nbFields;
    final boolean generatedId;

    String insertSQL;
    String updateSQL;

    public SQLInsert(String dbName, boolean generatedId, String idField, String... fieldnames) {
        this.generatedId = generatedId;
        this.nbFields = fieldnames.length;

        StringBuilder _insert = new StringBuilder();
        StringBuilder _update = new StringBuilder();

        _insert.append("INSERT INTO " + dbName + "(");
        _update.append("UPDATE " + dbName + " SET ");

        boolean first = true;
        for (String fieldname : fieldnames) {
            if (first) {
                first = false;
            } else {
                _insert.append(", ");
                _update.append(", ");
            }

            _insert.append(fieldname);

            _update.append(fieldname);
            _update.append("=?");

        }

        if (!generatedId) {
            _insert.append(", " + idField);
        }

        _insert.append(") VALUES(");
        first = true;
        for (String ignored : fieldnames) {
            if (first) {
                first = false;
            } else {
                _insert.append(",");
            }
            _insert.append("?");
        }

        if (!generatedId) {
            _insert.append(",?");
        }

        _insert.append(")");
        _update.append(" WHERE " + idField + "=?");

        insertSQL = _insert.toString();
        updateSQL = _update.toString();
    }

    public long execute(Connection connection, boolean update, Object id, Object... values) throws SQLException {
        if (values.length != nbFields) {
            throw new XPMIllegalArgumentException("Expected %d fields, got %d", nbFields, values.length);
        }

        try(PreparedStatement st = connection.prepareStatement(update ? updateSQL : insertSQL, Statement.RETURN_GENERATED_KEYS)) {
            // Set the values
            for (int i = 0; i < values.length; i++) {
                if (values[i] instanceof InputStream) {
                    st.setBlob(i + 1, (InputStream)values[i]);
                } else {
                    st.setObject(i + 1, values[i]);
                }
            }

            if (!generatedId || update) {
                // The ID is provided
                st.setObject(values.length + 1, id);
                st.executeUpdate();
             } else {
                // We need to get the ID
                st.executeUpdate();
                try (ResultSet generatedKeys = st.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getLong(1);
                    } else {
                        throw new SQLException("Creating object failed, no ID obtained.");
                    }
                }
            }

        }

        return -1;
    }
}