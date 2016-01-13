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

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * A factory for our annotations
 */
public class XPMTypeAdapterFactory implements AnnotatedTypeAdapterFactory {
    private ArrayList<Factory> registry = new ArrayList<>();

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeAttributes attributes, TypeToken<T> type) {
        // Predefined types
        if (type.getRawType() == File.class)
            return (TypeAdapter<T>) new FileAdapter();

        // Look at registry to find a proxy for annotations
        for(Factory factory: registry) {
            if (type.getRawType().isAssignableFrom(factory.field.getType())) {
                // Found one factory, take the annotations from this field instead
                // of the original one
                attributes = new FieldAttributes(factory.field);
                break;
            }
        }

        final ClassChooser annotation = attributes.getAnnotation(ClassChooser.class);
        if (annotation != null) {
            try {
                return new ClassChooserAdapter(gson, annotation);
            } catch(RuntimeException e) {
                throw new RuntimeException("Error while creating class chooser adapter for " + type);
            }
        }

        return null;
    }


    /** Explores the fields of the registry class
     * @param registryClass The registry class
     */
    public void addClass(Class<?> registryClass) {
        for(Field field: registryClass.getDeclaredFields()) {
            if (hasAnnotation(field, ClassChooser.class)) {
                registry.add(new Factory(field));
            }
        }
    }

    /**
     * Returns true if the annotated element has one of the annotations
     * @param ae
     * @param classes
     * @return
     */
    private boolean hasAnnotation(AnnotatedElement ae, Class<? extends Annotation>... classes) {
        for(Class<? extends Annotation> aClass: classes) {
            if (ae.getAnnotation(aClass) != null) {
                return true;
            }
        }
        return false;
    }

    static private class Factory {
        private Field field;

        public Factory(Field field) {
            this.field = field;
        }
    }
}
