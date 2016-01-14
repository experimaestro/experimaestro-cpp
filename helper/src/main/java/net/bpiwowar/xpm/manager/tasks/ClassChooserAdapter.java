package net.bpiwowar.xpm.manager.tasks;

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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.internal.bind.JsonTreeReader;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.stream;

/**
 * Generic object deserialization
 */
public class ClassChooserAdapter extends ReaderTypeAdapter {
    private final Gson gson;
    private final ClassChooserMode mode;
    private final Class<?> baseClass;
    private HashMap<Class<?>, ClassChooserInstance> types = new HashMap<>();

    public ClassChooserAdapter(Gson gson, Class<?> baseClass, ClassChooser annotation) {
        this.gson = gson;
        this.baseClass = baseClass;

        // Using instances
        for (Class<?> aClass : annotation.classes())
            if (!addClass(aClass)) {
                throw new RuntimeException(
                        "A provided instance is not a valid choice ("
                                + aClass + " for )");
            }

        // Using given instances
        for (ClassChooserInstance instance : annotation.instances()) {
            types.put(instance.instance(), instance);
        }

        // Using packages
        for (Class<?> zclass : annotation.classesOfPackage()) {
            Introspection.getClasses(
                    aClass -> {
                        addClass(aClass);
                        return false;
                    }, zclass.getPackage().getName(), 0);
        }

        if (annotation.inner()) {
            stream(baseClass.getClasses()).forEach(this::addClass);
        }

        this.mode = annotation.mode();

        if (types.isEmpty())
            throw new RuntimeException("No class provided");
    }

    private boolean addClass(Class<?> aClass) {
        final ClassChooserInstance annotation = aClass.getAnnotation(ClassChooserInstance.class);
        if (annotation != null && baseClass.isAssignableFrom(aClass)) {
            types.put(aClass, annotation);
        }
        return false;
    }

    String getType(JsonElement element) {
        // If string, use this
        if (element.isJsonPrimitive())
            return element.getAsString();

        // Get the type from object
        if (element instanceof JsonObject) {
            final JsonElement _type = ((JsonObject) element).get("type");
            if (_type == null) {
                throw new JsonParseException("No type defined for " + baseClass);
            }
            return _type.getAsString();
        }

        throw new AssertionError("No type defined");
    }

    @Override
    public Object read(JsonReader in) throws IOException {
        // Get the underlying JSON
        final JsonElement json;
        switch (in.peek()) {
            case STRING:
                json = new JsonPrimitive(in.nextString());
                break;
            case BEGIN_OBJECT:
                if (in instanceof JsonTreeReader) {
                    json = ((JsonTreeReader) in).getJsonObject();
                } else {
                    json = new JsonParser().parse(in).getAsJsonObject();
                }
                break;
            default:
                throw new AssertionError();
        }

        // Type (if needed)
        String type = null;
        Class<?> aClass = null;

        mainLoop:
        for (Map.Entry<Class<?>, ClassChooserInstance> entry : types.entrySet()) {
            Class<?> candidate = entry.getKey();
            ClassChooserInstance a = entry.getValue();

            ClassChooserMode mode = this.mode;
            if (a.mode() != ClassChooserMode.DEFAULT) {
                mode = a.mode();
            }
            switch (mode) {
                case DEFAULT:
                case TYPE:
                    if (type == null) {
                        type = getType(json);
                    }
                    String classType = a.name();
                    if (type.equals(classType)) {
                        return gson.fromJson(json, candidate);
                    }
                    break;

                case FIELDS:
                    // Check if all fields are in Json
                    if (json instanceof JsonObject) {
                        JsonObject jo = (JsonObject) json;
                        boolean match = true;
                        for (Field field : candidate.getDeclaredFields()) {
                            if (!jo.has(field.getName())) {
                                match = false;
                                break;
                            }
                        }
                        if (match) {
                            return gson.fromJson(json, candidate);
                        }
                    }
                    break;

                default:
                    throw new AssertionError(mode);
            }
        }

        throw new JsonParseException("Could not find a suitable candidate for " + json);
    }
}
