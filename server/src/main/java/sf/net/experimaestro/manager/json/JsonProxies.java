package sf.net.experimaestro.manager.json;

import bpiwowar.experiments.Run;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.sleepycat.persist.model.Persistent;
import com.sleepycat.persist.model.PersistentProxy;
import sf.net.experimaestro.utils.log.Logger;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Serialization of Json with Berkeley DB JE
 */
@Persistent
public class JsonProxies implements PersistentProxy<Json> {
    final static private Logger LOGGER = Logger.getLogger();

    String jsonString;

    @Override
    public void initializeProxy(Json json) {
        try {
            StringWriter writer = new StringWriter();
            json.write(writer);
            writer.close();
            jsonString = writer.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Json convertProxy() {
        final JsonReader jsonReader = new JsonReader(new StringReader(jsonString));
        try {
            final Json json = readNext(jsonReader);
            if (jsonReader.peek() != JsonToken.END_DOCUMENT) {
                throw new RuntimeException("Expected end of document");
            }
            return json;
        } catch (IOException e) {
            LOGGER.error(e, "Error while reading JSON string [%s]", jsonString);
            throw new RuntimeException(e);
        } catch(RuntimeException e) {
            LOGGER.error(e, "Error while reading JSON string [%s]", jsonString);
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

    @Persistent(proxyFor = JsonObject.class)
    static public class JsonObjectProxy extends JsonProxies {
    }

    @Persistent(proxyFor = JsonReal.class)
    static public class JsonRealProxy extends JsonProxies {
    }
}
