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
    private ClassInfo type;
    HashMap<String, AnnotationInfo> annotations = new HashMap<>();

    public FieldInfo(String name, ClassInfo type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public AnnotationInfo getAnnotationInfo(Class<?> annotationClass) {
        return annotations.get(annotationClass.getName());
    }

    public ClassInfo getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
