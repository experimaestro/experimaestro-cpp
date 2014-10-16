package net.bpiwowar.experimaestro.tasks;

import com.google.gson.*;
import com.google.gson.internal.bind.JsonTreeReader;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.HashMap;

/**
 * Generic object deserialization
 */
public class ClassChooserAdapter extends ReaderTypeAdapter {
    private final Gson gson;
    private HashMap<Object, Class<?>> types = new HashMap<>();

    public ClassChooserAdapter(Gson gson, ClassChooser annotation) {
        this.gson = gson;
        // Using instances
        for (Class<?> aClass : annotation.classes())
            if (!addClass(aClass)) {
                throw new RuntimeException(
                        "A provided instance is not a valid choice ("
                                + aClass + " for )");
            }

        // Using packages
        for (Class<?> zclass : annotation.classesOfPackage()) {
            Introspection.getClasses(
                    aClass -> {
                        addClass(aClass);
                        return false;
                    }, zclass.getPackage().getName(), 0);
        }

        if (types.isEmpty())
            throw new RuntimeException("No class provided");
    }

    private boolean addClass(Class<?> aClass) {
        final ClassChooserInstance annotation = aClass.getAnnotation(ClassChooserInstance.class);
        types.put(annotation.name(), aClass);
        return false;
    }

    @Override
    public Object read(JsonReader in) throws IOException {
        // If string, use this
        if (in.peek() == JsonToken.STRING) {
            final String type = in.nextString();
            final Class<?> aClass = types.get(type);
            if (aClass == null) {
                throw new JsonParseException("No type " + type + " defined");
            }
            return gson.fromJson(new JsonObject(), aClass);
        }

        // Get the Json object
        final JsonObject json;
        if (in instanceof JsonTreeReader) {
            json = ((JsonTreeReader) in).getJsonObject();
        } else {
            json = new JsonParser().parse(in).getAsJsonObject();
        }

        // Get the type
        final JsonElement _type = json.get("type");
        if (_type == null) {
            throw new JsonParseException("No type defined");
        }
        String type = _type.getAsString();

        // Get the class of the object to create
        final Class<?> aClass = types.get(type);
        if (aClass == null) {
            throw new JsonParseException("No type " + type + " defined");
        }

        return gson.fromJson(json, aClass);
    }
}
