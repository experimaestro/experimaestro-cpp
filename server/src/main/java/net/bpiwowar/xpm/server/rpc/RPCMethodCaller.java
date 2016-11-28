package net.bpiwowar.xpm.server.rpc;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.bpiwowar.xpm.exceptions.XPMCommandException;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 *
 */
public class RPCMethodCaller extends NamedRPCCaller<MethodArgumentDescriptor> {
    /**
     * Method
     */
    Method method;


    public RPCMethodCaller(Method method) {
        this.method = method;
        final Type[] types = method.getGenericParameterTypes();
        Annotation[][] annotations = method.getParameterAnnotations();
        mainLoop:
        for (int i = 0; i < annotations.length; i++) {
            for (int j = 0; j < annotations[i].length; j++) {
                if (annotations[i][j] instanceof RPCArgument) {
                    final RPCArgument annotation = (RPCArgument) annotations[i][j];
                    final String name = annotation.name();
                    arguments.put(name, new MethodArgumentDescriptor(annotation, i));
                    continue mainLoop;
                }
            }

            throw new XPMRuntimeException("No annotation for %dth argument of %s", i + 1, method);
        }
    }

    @Override
    public Object call(Object o, JsonObject p) throws InvocationTargetException, IllegalAccessException {
        Object[] args = new Object[method.getParameterCount()];
        Gson gson = new Gson();
        final Type[] types = method.getGenericParameterTypes();
        for (MethodArgumentDescriptor descriptor : arguments.values()) {
            final JsonElement jsonElement = p.get(descriptor.name);
            final Type type = types[descriptor.position];
            try {
                args[descriptor.position] = gson.fromJson(jsonElement, type);
            } catch (RuntimeException e) {
                throw new XPMCommandException(e).addContext("while processing parameter %s", descriptor.name);
            }
        }

        return method.invoke(o, args);
    }

    @Override
    public Class<?> getDeclaringClass() {
        return method.getDeclaringClass();
    }
}
