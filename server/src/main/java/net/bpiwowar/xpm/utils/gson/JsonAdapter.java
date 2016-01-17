package net.bpiwowar.xpm.utils.gson;

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

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.bpiwowar.xpm.manager.json.*;

import java.io.IOException;

/**
 * Json adapter
 */
public class JsonAdapter extends TypeAdapter<Json> {
    @Override
    public void write(JsonWriter out, Json value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            value.write(out);
        }
    }

    @Override
    public Json read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            return null;
        }
        return readValue(in);
    }

    private Json readValue(JsonReader in) throws IOException {
        final JsonToken peek = in.peek();
        switch(peek) {
            case BEGIN_ARRAY:
                in.beginArray();
                JsonArray array = new JsonArray();
                while (in.peek() != JsonToken.END_ARRAY) {
                    array.add(readValue(in));
                }
                in.endArray();
                return array;

            case BEGIN_OBJECT:
                in.beginObject();
                JsonObject object = new JsonObject();
                while (in.peek() != JsonToken.END_OBJECT) {
                    final String name = in.nextName();
                    final Json value = readValue(in);
                    object.put(name, value);
                }
                in.endObject();
                return object;

            case BOOLEAN:
                return JsonBoolean.of(in.nextBoolean());

            case NUMBER:
                final double value = in.nextDouble();
                if (value == Math.round(value))
                    return new JsonInteger((long) value);
                return new JsonReal(value);

            case STRING:
                return new JsonString(in.nextString());

            case NULL:
                return JsonNull.getSingleton();

            case NAME:
            case END_OBJECT:
            case END_ARRAY:
            case END_DOCUMENT:
                throw new AssertionError("Not expecting " + peek);
        }
        return null;
    }
}