package sf.net.experimaestro.utils.introspection;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;

/**
 * Created by bpiwowar on 3/10/14.
 */
public class FieldInfo implements AnnotatedElement {
    private String name;
    HashMap<String, AnnotationInfo> annotations = new HashMap<>();

    public FieldInfo(String name) {
        this.name = name;
    }

    @Override
    public AnnotationInfo getAnnotationInfo(Class<?> annotationClass) {
        return annotations.get(annotationClass.getName());
    }
}
