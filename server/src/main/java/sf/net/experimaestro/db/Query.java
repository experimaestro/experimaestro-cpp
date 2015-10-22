package sf.net.experimaestro.db;

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

import com.google.common.base.Joiner;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Query building
 */
public class Query {

    String queryString;

    private ArrayList<Field> select = new ArrayList<>();

    private ArrayList<Predicate> predicates = new ArrayList<>();

    public void add(Field field) {
        this.select.add(field);
    }

    public Query where(Predicate predicate) {
        this.predicates.add(predicate);
        return this;
    }

    public String toString() {
        if (queryString == null) {
            // Get tables


            //
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT ");
            Joiner.on(") and (").appendTo(sb, predicates);
            sb.append(" FROM ");
            if (!predicates.isEmpty()) {
                sb.append(" WHERE (");
                Joiner.on(") and (").appendTo(sb, predicates);
                sb.append(')');
            }
        }
        return queryString;
    }

    public void execute(Connection connection) throws SQLException {
        final String query = toString();
        try(PreparedStatement st = connection.prepareStatement(query)) {
            // Set holders

            // Execute query
            st.execute();
        }
    }
}
