package sf.net.experimaestro.manager.js;

import org.mozilla.javascript.Context;
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
}
