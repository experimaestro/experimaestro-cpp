package net.bpiwowar.xpm.server.rpc;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.bpiwowar.xpm.exceptions.XPMCommandException;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

/**
 *
 */
class RPCClassCaller extends NamedRPCCaller<ClassArgumentDescriptor> {
    private final Class<?> rpcClass;
    private final Constructor<?> constructor;

    public RPCClassCaller(Class<?> rpcClass) {
        this.rpcClass = rpcClass;
        if (!JsonCallable.class.isAssignableFrom(rpcClass)) {
            throw new AssertionError("An RPC method class should implement the JsonCallable interface");
        }

        try {
            if (!Modifier.isStatic(rpcClass.getModifiers())) {
                constructor = rpcClass.getConstructor(rpcClass.getEnclosingClass());
            } else {
                constructor = null; // rpcClass.getConstructor();
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        for (Field field : rpcClass.getDeclaredFields()) {
            final RPCArgument annotation = field.getAnnotation(RPCArgument.class);

            if (annotation != null) {
                final ClassArgumentDescriptor descriptor = new ClassArgumentDescriptor(annotation, field);
                final ClassArgumentDescriptor old = arguments.put(descriptor.name, descriptor);
                if (old != null) {
                    throw new XPMRuntimeException("Parameter %s was already defined for %s", descriptor.name, rpcClass);
                }
            }
        }
    }

    @Override
    public Object call(Object o, JsonObject p) throws Throwable {
        Gson gson = new Gson();
        final JsonCallable object;
        if (!Modifier.isStatic(rpcClass.getModifiers())) {
            object = (JsonCallable) constructor.newInstance(o);
        } else {
            object = (JsonCallable) rpcClass.newInstance();
        }

        for (ClassArgumentDescriptor descriptor : arguments.values()) {
            final JsonElement jsonElement = p.get(descriptor.name);
            final Type type = descriptor.field.getGenericType();
            try {
                Object value = gson.fromJson(jsonElement, type);
                if (!descriptor.field.isAccessible()) {
                    descriptor.field.setAccessible(true);
                    descriptor.field.set(object, value);
                    descriptor.field.setAccessible(false);
                } else {
                    descriptor.field.set(object, value);
                }
            } catch (RuntimeException e) {
                throw new XPMCommandException(e).addContext("while processing parameter %s", descriptor.name);
            }
        }

        return object.call();
    }

    @Override
    public Class<?> getDeclaringClass() {
        return rpcClass.getEnclosingClass();
    }
}
