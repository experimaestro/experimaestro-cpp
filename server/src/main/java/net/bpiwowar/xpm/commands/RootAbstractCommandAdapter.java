package net.bpiwowar.xpm.commands;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.bpiwowar.xpm.utils.UUIDObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Root command (de)serialization
 */
public class RootAbstractCommandAdapter implements TypeAdapterFactory {
    private Gson gson;

    public class Adapter extends TypeAdapter<AbstractCommand> {
        @Override
        public void write(JsonWriter out, AbstractCommand command) throws IOException {
            out.beginArray();

            out.value(command.getUUID());

            // Write commands and components first, indexed by their UUID
            HashSet<String> uuids = new HashSet<>();
            out.beginObject();
            save(out, uuids, UUIDObject.class, command.commands());
            save(out, uuids, UUIDObject.class, command.allComponents());
            out.endObject();


            // Write the command itself (as UUIDs -> extra data)
            out.beginObject();
            final Iterator<UUIDObject> iterator = Stream.concat(command.commands(), command.allComponents()).iterator();
            while (iterator.hasNext()) {
                final UUIDObject object = iterator.next();
                out.name(object.getUUID());
                out.beginObject();
                object.postJSONSave(out);
                out.endObject();
            }
            out.endObject();

            out.endArray();
        }

        public void save(JsonWriter out, HashSet<String> uuids, Class<? extends UUIDObject> typeOfSrc, Stream<? extends UUIDObject> stream) throws IOException {
            final Iterator<? extends UUIDObject> iterator = stream.iterator();
            while (iterator.hasNext()) {
                UUIDObject object = iterator.next();
                final String uuid = object.getUUID();
                if (!uuids.contains(uuid)) {
                    uuids.add(uuid);
                    out.name(uuid);
                    RootAbstractCommandAdapter.this.gson.toJson(object, typeOfSrc, out);
                }
            }

        }

        @Override
        public AbstractCommand read(JsonReader in) throws IOException {
            in.beginArray();

            String commandUUID = in.nextString();

            // Read commands with their UUIDs
            in.beginObject();
            Map<String, UUIDObject> uuids = new HashMap<>();
            while (in.peek() != JsonToken.END_OBJECT) {
                final String uuid = in.nextName();
                final UUIDObject command = gson.fromJson(in, UUIDObject.class);
                uuids.put(uuid, command);
                command.setUUID(uuid);
            }
            in.endObject();

            if (!uuids.containsKey(commandUUID))
                throw new AssertionError("Serialized JSON does not contain UUID " + commandUUID);

            // Complete the serialization
            in.beginObject();
            while (in.peek() != JsonToken.END_OBJECT) {
                String uuid = in.nextName();
                final UUIDObject o = uuids.get(uuid);
                in.beginObject();
                while (in.peek() != JsonToken.END_OBJECT) {
                    final String name = in.nextName();
                    o.postJSONLoad(uuids, in, name);
                }
                in.endObject();
            }
            in.endObject();

            in.endArray();
            return (AbstractCommand) uuids.get(commandUUID);
        }
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        this.gson = gson;
        assert AbstractCommand.class.isAssignableFrom(type.getRawType());
        return (TypeAdapter<T>) new Adapter();
    }
}
