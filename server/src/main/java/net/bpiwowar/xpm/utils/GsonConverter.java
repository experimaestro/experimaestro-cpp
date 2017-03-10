package net.bpiwowar.xpm.utils;

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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.bpiwowar.xpm.connectors.Connector;
import net.bpiwowar.xpm.utils.gson.ByteArrayAdapter;
import net.bpiwowar.xpm.utils.gson.ConnectorAdapter;
import net.bpiwowar.xpm.utils.gson.JsonPathAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Path;

/**
 * Converts a command into a JSON string
 */
public class GsonConverter {
    final static private Logger LOGGER = LogManager.getFormatterLogger();

    /**
     * The builder to use in most cases
     */
    final static public GsonBuilder defaultBuilder = new GsonBuilder();

    /**
     * Used to serialize connectors
     */
    final static public GsonBuilder connectorBuilder = new GsonBuilder();

    static {
        // Last has more priority
        defaultBuilder.registerTypeAdapterFactory(new AbstractObjectFactory());
        connectorBuilder.registerTypeAdapterFactory(new AbstractObjectFactory(Connector.class));

        for (GsonBuilder gsonBuilder : new GsonBuilder[]{defaultBuilder, connectorBuilder}) {
            gsonBuilder.registerTypeHierarchyAdapter(Path.class, new JsonPathAdapter());
            gsonBuilder.registerTypeAdapter(byte[].class, new ByteArrayAdapter());

            gsonBuilder.setExclusionStrategies(new GsonExclusionStrategy());
        }

        defaultBuilder.registerTypeHierarchyAdapter(Connector.class, new ConnectorAdapter());
    }

    private static class AbstractObjectFactory implements TypeAdapterFactory {
        final Class<?>[] classes;

        public AbstractObjectFactory(Class<?>... classes) {
            this.classes = classes;
        }

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            final Class<? super T> rawType = type.getRawType();

            for (Class<?> aClass : classes) {
                if (aClass.isAssignableFrom(rawType)) {
                    return null;
                }
            }


            if (rawType.getAnnotation(JsonAbstract.class) != null) {
                return new AbstractObjectAdapter(gson, type);
            }

            if (!rawType.isArray() && !rawType.isPrimitive() && (rawType.isInterface() || Modifier.isAbstract(rawType.getModifiers()))) {
                if (rawType.getCanonicalName().startsWith("net.bpiwowar.xpm")) {
                    LOGGER.warn("Not using Abstract Object Adapter for %s", rawType);
                }
            }
            return null;
        }

    }

    /**
     * Objects are represented as an array containing
     * the java class (fully qualified) and the serialized Json
     */
    private static class AbstractObjectAdapter extends TypeAdapter {
        private final Gson gson;
        // This is for debug - remove ?
        private final TypeToken type;

        public <T> AbstractObjectAdapter(Gson gson, TypeToken<T> type) {
            this.gson = gson;
            this.type = type;
        }

        @Override
        public void write(JsonWriter out, Object value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.beginArray();
                final Class<?> aClass = value.getClass();
                assert !aClass.isArray();
                out.value(aClass.getName());
                LOGGER.debug("Serializing %s", value.getClass());
                gson.toJson(value, value.getClass(), out);
                out.endArray();
            }
        }

        @Override
        public Object read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                return null;
            }

            try {
                in.beginArray();
                final String classname = in.nextString();
                final Object o;
                try {
                    final Class<?> aClass = Class.forName(classname);
                    o = gson.fromJson(in, aClass);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                in.endArray();
                return o;
            } catch (IllegalArgumentException e) {
                LOGGER.error("Error while reading type %s", this.type);
                throw e;
            }

        }
    }
}
