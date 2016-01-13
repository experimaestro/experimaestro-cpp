package net.bpiwowar.xpm.manager.scripting;

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


import net.bpiwowar.xpm.utils.Functional;

import java.lang.reflect.Field;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Access to a property for a given class
 */
public class PropertyAccess {
    Function<Object, Object> getter;

    BiConsumer<Object, Object> setter;

    public PropertyAccess(Function<Object, Object> getter, BiConsumer<Object, Object> setter) {
        this.getter = getter;
        this.setter = setter;
    }

    public PropertyAccess() {

    }

    public ScriptingReference get(final Object object) {
        return new ObjectPropertyReference(getter, setter, object);
    }

    public void set(Object object, Object value) {
        setter.accept(object, value);
    }

    public boolean canSet() {
        return setter != null;
    }

    static public class FieldAccess extends PropertyAccess {
        public FieldAccess(Field field) {
            super(
                    Functional.propagateFunction(x -> field.get(x)),
                    Functional.propagate((o, v) -> field.set(o, v))
            );
        }
    }

}
