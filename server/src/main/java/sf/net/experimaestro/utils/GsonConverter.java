package sf.net.experimaestro.utils;

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
import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.utils.gson.ByteArrayAdapter;
import sf.net.experimaestro.utils.gson.ConnectorAdapter;
import sf.net.experimaestro.utils.gson.JsonAdapter;
import sf.net.experimaestro.utils.gson.JsonPathAdapter;
import sf.net.experimaestro.utils.log.Logger;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.nio.file.Path;

/**
 * Converts a command into a JSON string
 */
public class GsonConverter {
    final static private Logger LOGGER = Logger.getLogger();

    final static public GsonBuilder builder = new GsonBuilder();
    final static public GsonBuilder rawBuilder = new GsonBuilder();

    static {
        // Last has more priority
        for (GsonBuilder gsonBuilder : new GsonBuilder[]{builder, rawBuilder}) {
            gsonBuilder.registerTypeAdapterFactory(new AbstractObjectFactory());

            gsonBuilder.registerTypeHierarchyAdapter(Json.class, new JsonAdapter());
            gsonBuilder.registerTypeHierarchyAdapter(Path.class, new JsonPathAdapter());
            gsonBuilder.registerTypeAdapter(byte[].class, new ByteArrayAdapter());

            gsonBuilder.setExclusionStrategies(new GsonExclusionStrategy());
        }

        builder.registerTypeHierarchyAdapter(Connector.class, new ConnectorAdapter());

    }

    private static class AbstractObjectFactory implements TypeAdapterFactory {
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            final Class<? super T> rawType = type.getRawType();

            if (rawType.getAnnotation(JsonAbstract.class) != null) {
                return new AbstractObjectAdapter(gson, type);
            }

            if (!rawType.isArray() && !rawType.isPrimitive() && (rawType.isInterface() || Modifier.isAbstract(rawType.getModifiers()))) {
                if (rawType.getCanonicalName().startsWith("sf.net.experimaestro")) {
                    LOGGER.warn("Not using Abstract Object Adapter for %s", rawType);
                }
            }
            return null;
        }

    }

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
