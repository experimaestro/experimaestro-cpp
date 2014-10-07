package net.bpiwowar.experimaestro.tasks;

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


    /** Explores the fields of the registry class */
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
