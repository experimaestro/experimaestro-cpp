package sf.net.experimaestro.manager.json;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Serialization of Json with Berkeley DB JE
 */
@Converter
public class JsonConverter implements AttributeConverter<Json, String> {

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
            throw new RuntimeException(e);
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

            case NULL:
                return JsonNull.getSingleton();

            case NUMBER:
                return new JsonReal(jsonReader.nextDouble());

            default:
                throw new RuntimeException("Cannot handle GSON token type: " + token.name());

        }
    }


}
