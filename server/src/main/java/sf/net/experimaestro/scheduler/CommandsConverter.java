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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.utils.gson.JsonAdapter;
import sf.net.experimaestro.utils.gson.JsonPathAdapter;

import javax.persistence.AttributeConverter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Path;

/**
 * Converts a command into a JSON string
 */
public class CommandsConverter implements AttributeConverter<Commands, String> {

    final static public GsonBuilder builder = new GsonBuilder();

    static {
        builder.registerTypeAdapter(Json.class, new JsonAdapter());
        builder.registerTypeAdapter(Path.class, new JsonPathAdapter());
        builder.registerTypeAdapterFactory(new AbstractObjectFactory());
    }

    @Override
    public String convertToDatabaseColumn(Commands commands) {
        Gson gson = builder.create();
        final String json = gson.toJson(commands);
        return json;
    }

    @Override
    public Commands convertToEntityAttribute(String json) {
        Gson gson = builder.create();
        final Commands commands = gson.fromJson(json, Commands.class);
        return commands;
    }

    private static class AbstractObjectFactory implements TypeAdapterFactory {
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            final Class<? super T> rawType = type.getRawType();
            if (!rawType.isArray() && (rawType.isInterface() || Modifier.isAbstract(rawType.getModifiers())))
                return new AbstractObjectAdapter(gson, type);
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
            out.beginArray();
            final Class<?> aClass = value.getClass();
            assert ! aClass.isArray();
            out.value(aClass.getName());
            gson.toJson(value, value.getClass(), out);
            out.endArray();
        }

        @Override
        public Object read(JsonReader in) throws IOException {
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
        }
    }
}
