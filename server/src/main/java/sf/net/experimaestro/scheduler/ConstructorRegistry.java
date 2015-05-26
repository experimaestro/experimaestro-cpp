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

import com.sun.tools.internal.jxc.ap.Const;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.exceptions.XPMRuntimeException;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * A registry of constructors
 */
public class ConstructorRegistry<T> {
    private final Class[] parameterTypes;

    Long2ObjectLinkedOpenHashMap<Constructor<? extends T>> map = new Long2ObjectLinkedOpenHashMap<>();

    public ConstructorRegistry(Class[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    ConstructorRegistry<T> add(Class<? extends T>... classes) {
        for (Class<? extends T> aClass : classes) {
            try {
                map.put(DatabaseObjects.getTypeValue(aClass.getAnnotation(TypeIdentifier.class).value()),
                        aClass.getConstructor(parameterTypes));
            } catch (NoSuchMethodException e) {
                throw new XPMRuntimeException(e, "Cannot add class %s to registry", aClass);
            }
        }
        return this;
    }

    public Constructor<? extends T> get(long typeId) {
        return map.get(typeId);
    }
}
