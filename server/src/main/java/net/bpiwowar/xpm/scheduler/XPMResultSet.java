/*
 *
 *  * This file is part of experimaestro.
 *  * Copyright (c) 2015 B. Piwowarski <benjamin@bpiwowar.net>
 *  *
 *  * experimaestro is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * experimaestro is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package net.bpiwowar.xpm.scheduler;

import net.bpiwowar.xpm.exceptions.WrappedSQLException;

import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A wrapper for a result set
 */
public class XPMResultSet implements AutoCloseable {
    private final XPMStatement xpmStatement;
    private final ResultSet resultSet;

    public XPMResultSet(XPMStatement xpmStatement, ResultSet resultSet) {
        this.xpmStatement = xpmStatement;
        this.resultSet = resultSet;
    }

    public boolean next() throws SQLException {
        return resultSet.next();
    }

    @Override
    public void close() throws SQLException {
        resultSet.close();
        xpmStatement.close();
    }

    public InputStream getBinaryStream(int index) throws SQLException {
        return resultSet.getBinaryStream(index);
    }

    /**
     * Get the string for a given column
     * @see ResultSet#getString(int)
     * @throws WrappedSQLException
     */
    public String getString(int columnIndex) throws WrappedSQLException {
        try {
            return resultSet.getString(columnIndex);
        } catch (SQLException e) {
            throw new WrappedSQLException(e);
        }
    }
}
