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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

/**
 *
 */
public class DbUtils {
    public static void processOne(PreparedStatement st, Consumer<ResultSet> c) throws SQLException {
        st.execute();
        ResultSet resultSet = getFirst(st);
        c.accept(resultSet);
        if (resultSet.next()) {
            throw new SQLException("Too many results for query " + st.toString());
        }
    }

    public static ResultSet getFirst(PreparedStatement st) throws SQLException {
        st.execute();
        ResultSet resultSet = st.getResultSet();
        if (!resultSet.next()) {
            throw new SQLException("No result for query " + st.toString());
        }
        return resultSet;
    }
}
