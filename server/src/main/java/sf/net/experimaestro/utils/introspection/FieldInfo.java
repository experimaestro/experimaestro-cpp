package sf.net.experimaestro.utils.introspection;

import java.util.HashMap;

/**
 * Created by bpiwowar on 3/10/14.
 */
public class FieldInfo implements AnnotatedElement {
    HashMap<String, AnnotationInfo> annotations = new HashMap<>();
    private String name;
    private ClassInfo type;

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
