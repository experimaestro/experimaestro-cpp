package sf.net.experimaestro.manager.js;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrappedException;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.utils.JSUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Wraps a method of the object
 */
class MethodFunction implements Callable {
    ArrayList<Method> methods = new ArrayList<>();

    public MethodFunction() {
    }

    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double max = 0;
        Method argmax = null;
        for (Method method : methods) {
            double score = score(method, args);
            if (score > max) {
                max = score;
                argmax = method;
            }
        }

        if (argmax == null)
            throw new ExperimaestroRuntimeException("Could not find a matching method");

        // Call the method

        try {
            args = transform(cx, scope, argmax, args);
            final Object invoke = argmax.invoke(thisObj, args);
            return cx.getWrapFactory().wrap(cx, scope, invoke, null);
        } catch (Throwable e) {
            if (e.getCause() != null)
                e = e.getCause();
            throw new WrappedException(e);
        }

    }

    private Object[] transform(Context cx, Scriptable scope, Method method, Object[] args) {
        final boolean useScope = method.getAnnotation(JSFunction.class).scope();
        int offset = useScope ? 2 : 0;
        if (useScope) {
            Object[] newArgs = new Object[args.length + 2];
            System.arraycopy(args, 0, newArgs, 2, args.length);
            args = newArgs;
            args[0] = cx;
            args[1] = scope;
        }

        final Class<?>[] types = method.getParameterTypes();
        for (int i = offset; i < types.length; i++) {
            args[i] = JSUtils.unwrap(args[i]);
        }

        return args;
    }

    private double score(Method method, Object[] args) {
        final boolean scope = method.getAnnotation(JSFunction.class).scope();
        int offset = scope ? 2 : 0;

        final Class<?>[] types = method.getParameterTypes();
        if (args.length != types.length - offset)
            return 0;

        double score = 1;
        for (int i = offset; i < types.length && score > 0; i++) {
            final Object o = JSUtils.unwrap(args[i - offset]);
            if (types[i].isAssignableFrom(o.getClass()))
                continue;
            score = 0;
            break;
        }

        return score;
    }
}
