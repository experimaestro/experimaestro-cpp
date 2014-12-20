package sf.net.experimaestro.manager.json;

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

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import sf.net.experimaestro.utils.log.Logger;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Serialization of Json
 */
@Converter
public class JsonConverter implements AttributeConverter<Json, String> {
    final static private Logger LOGGER = Logger.getLogger();
    @Override
    public String convertToDatabaseColumn(Json json) {
        try {
            StringWriter writer = new StringWriter();
            json.write(writer);
            writer.close();
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Json convertToEntityAttribute(String dbData) {
        final JsonReader jsonReader = new JsonReader(new StringReader(dbData));

        try {
            final Json json = readNext(jsonReader);
            if (jsonReader.peek() != JsonToken.END_DOCUMENT) {
                throw new RuntimeException("Expected end of document");
            }
            return json;
        } catch (IOException e) {
            LOGGER.error(e, "Error while reading JSON string [%s]", dbData);
            throw new RuntimeException(e);
        } catch(RuntimeException e) {
            LOGGER.error(e, "Error while reading JSON string [%s]", dbData);
            throw e;
        }
    }

    private Json readNext(JsonReader jsonReader) throws IOException {
        final JsonToken token = jsonReader.peek();
        switch (token) {
            case BEGIN_ARRAY: {
                jsonReader.beginArray();
                JsonArray array = new JsonArray();
                while (jsonReader.peek() != JsonToken.END_ARRAY) {
                    array.add(readNext(jsonReader));
                }
                jsonReader.endArray();
                return array;
            }

            case BEGIN_OBJECT: {
                jsonReader.beginObject();
                JsonObject object = new JsonObject();
                while (jsonReader.peek() != JsonToken.END_OBJECT) {
                    final String name = jsonReader.nextName();
                    final Json value = readNext(jsonReader);
                    object.put(name, value);
                }
                jsonReader.endObject();
                return object;
            }

            case BOOLEAN:
                return new JsonBoolean(jsonReader.nextBoolean());

            case STRING:
                return new JsonString(jsonReader.nextString());

            case NULL: {
                jsonReader.nextNull();
                return JsonNull.getSingleton();
            }

            case NUMBER:
                return new JsonReal(jsonReader.nextDouble());

            default:
                throw new RuntimeException("Cannot handle GSON token type: " + token.name());

        }
    }


}
