package sf.net.experimaestro.utils.introspection;

import java.util.HashMap;

/**
 * Created by bpiwowar on 3/10/14.
 */
public class AnnotationInfo {
    final public ClassInfo annotationClass;
    final public HashMap<String, Object> content = new HashMap<>();

    public AnnotationInfo(ClassInfo annotationClass) {
        this.annotationClass = annotationClass;
    }

    void setAttribute(String name, Object value) {
        content.put(name, value);
    }
}
