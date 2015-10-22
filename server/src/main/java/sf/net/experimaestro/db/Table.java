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

import com.google.common.collect.HashMultimap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Table related utilities
 */
public class Table {
    static HashMap<Class<?>, Table> tables = new HashMap<>();

    private String name;

    String sqlCreate() {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE " + name + " (");
        boolean first = true;

        for (Field field : fields) {
            first = appendnotfirst(sb, first, ",\n");
            sb.append(field.name());
            sb.append(" ");
            sb.append(field.type());
            if (field.notNull) sb.append(" NOT NULL");
        }

        for (Map.Entry<Table, Collection<Field>> reference : references.asMap().entrySet()) {
            sb.append(",\n");
            sb.append("FOREIGN KEY (");
            first = true;
            for (Field field : reference.getValue()) {
                first = appendnotfirst(sb, first, ",\n");
                sb.append(field.name());
            }

            sb.append(") REFERENCES " + reference.getKey().name);
        }


        sb.append("\n)");



        return sb.toString();
    }

    static private boolean appendnotfirst(StringBuilder sb, boolean first, String s) {
        if (!first) sb.append(s);
        return false;
    }

    ArrayList<Field> fields = new ArrayList<>();

    protected Iterable<? extends Field> fields() {
        return null;
    }

    protected HashMultimap<Table, Field> references = HashMultimap.create();

    protected Table() {
        this.name = this.getClass().getSimpleName();

        for (java.lang.reflect.Field field : this.getClass().getDeclaredFields()) {
            if (Field.class.isAssignableFrom(field.getType())) {
                final Field dbField = new Field(field);
                fields.add(dbField);

                // --- References
                final References references = field.getAnnotation(References.class);
                if (references != null) {
                    final Class<? extends Table> aClass = references.value();
                    this.references.put(create(aClass), dbField);
                }

            }
        }


    }

    static public <T extends Table> T create(Class<T> aClass) {
        T table = (T) tables.get(aClass);
        if (table == null) {
            try {
                table = aClass.newInstance();
                tables.put(aClass, table);
                System.err.println(table.sqlCreate());
            } catch (InstantiationException | IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }

        return table;
    }

    public TableReference ref() {
        return new TableReference(this);
    }


    public void create(Connection connection) throws SQLException {
        try(PreparedStatement st = connection.prepareStatement(sqlCreate())) {
            st.execute();
        }
    }
}
