package sf.net.experimaestro.manager.js;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaClass;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

/**
 * An object with exposed fields
 */
@JSObjectDescription(name = "@WrappedJavaObject")
public class WrappedJavaObject extends JSBaseObject implements Wrapper {
    private final Object object;

    public WrappedJavaObject(Context cx, Scriptable scope, Object object) {
        super(object.getClass());
        this.object = object;
    }

    @Override
    public Object unwrap() {
        return object;
    }

    @Override
    protected Object thisObject() {
        return this.object;
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        return getClassName();
    }

    public static class WrappedClass extends NativeJavaClass {
        private Class<?> javaClass;

        public WrappedClass(Class<?> javaClass) {
            this.javaClass = javaClass;
        }

        @Override
        public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
            ClassDescription description = analyzeClass((Class) javaClass);
            String className = JSBaseObject.getClassName((Class) javaClass);
            ConstructorFunction constructorFunction = new ConstructorFunction(className, description.constructors);
            Object object = constructorFunction.call(cx, scope, null, args);

            return new WrappedJavaObject(cx, scope, object);

        }

    }
}
