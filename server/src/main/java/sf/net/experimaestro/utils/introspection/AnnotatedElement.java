package sf.net.experimaestro.utils.introspection;

import sf.net.experimaestro.exceptions.XPMRuntimeException;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by bpiwowar on 3/10/14.
 */
public interface AnnotatedElement {
    default public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        final AnnotationInfo info = getAnnotationInfo(annotationClass);

        if (info == null)
            return null;

        return (T) Proxy.newProxyInstance(
                annotationClass.getClassLoader(),
                new Class[]{annotationClass},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        // Get value
                        final String name = method.getName();
                        final Object o = info.content.get(name);
                        if (o != null) {
                            return o;
                        }

                        // Get default value
                        try {
                            return annotationClass.getMethod(name).getDefaultValue();
                        } catch (NoSuchMethodException e) {
                            throw new XPMRuntimeException(e);
                        }
                    }
                });
    }

    public AnnotationInfo getAnnotationInfo(Class<?> annotationClass);
}
