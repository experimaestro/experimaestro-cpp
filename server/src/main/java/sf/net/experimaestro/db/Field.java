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

import sf.net.experimaestro.exceptions.XPMIllegalArgumentException;

import java.lang.reflect.ParameterizedType;
import java.sql.Blob;

/**
 * A database field
 */
public class Field<T> extends Value implements Order, FieldReference {
    private final FieldType type;

    private final int maxsize;

    private final boolean identity;

    private String name;

    boolean notNull;

    public Field(java.lang.reflect.Field field) {
        this.name = field.getName();
        this.notNull = field.getAnnotation(NotNull.class) != null;

        final MaxSize maxSizeAnnotation = field.getAnnotation(MaxSize.class);
        this.maxsize = maxSizeAnnotation != null ? maxSizeAnnotation.value() : 0;

        this.identity = field.getAnnotation(Identity.class) != null;

        // --- Process type
        ParameterizedType fieldtype = (ParameterizedType) field.getGenericType();
        final Class type = (Class) fieldtype.getActualTypeArguments()[0];

        if (instanceOf(type, Integer.class)) {
            this.type = FieldType.Integer;
        } else if (instanceOf(type, IntegerType.class)) {
            this.type = FieldType.Long;
        } else if (instanceOf(type, String.class)) {
            this.type = FieldType.String;
        } else if (instanceOf(type, Blob.class)) {
            this.type = FieldType.Blob;
        } else if (instanceOf(type, Table.class)) {
            this.type = null;
        } else {
            throw new XPMIllegalArgumentException("Field %s could not be mapped to a type [%s]", field, fieldtype);
        }
    }

    private boolean instanceOf(Class<?> type, Class<?>... classes) {
        for (Class<?> aClass : classes) {
            if (aClass.isAssignableFrom(type)) return true;
        }
        return false;
    }

    public String name() {
        return name;
    }

    public String type() {
        if (identity) return "IDENTITY";

        switch(type) {
            case Blob:
                return "BLOB";
            case Long:
                return "BIGINT";
            case Integer:
                return "INT";
            case String:
                return "VARCHAR(" + maxsize + ")";
        }

        throw new XPMIllegalArgumentException("Unsupported type: %s", type);
    }
}
