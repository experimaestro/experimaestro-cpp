package sf.net.experimaestro.manager.js;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.mutable.MutableInt;
import org.mozilla.javascript.*;
import sf.net.experimaestro.exceptions.XPMRhinoException;
import sf.net.experimaestro.utils.Output;

import java.lang.reflect.Array;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.StrictMath.min;

/**
 *
 * Base class for javascript methods or constructors
 *
 * Created by bpiwowar on 10/9/14.
 */
public abstract class GenericFunction implements Callable {

    abstract static public class Declaration<T extends Executable> {
        private final T executable;

        public Declaration(T executable) {
            this.executable = executable;
        }

        public Executable executable() {
            return executable;
        }

        public abstract Object invoke(Object[] transformedArgs) throws InvocationTargetException, IllegalAccessException, InstantiationException;

    }


    /** Get the name of the method or constructor */
    protected abstract String getName();

    protected abstract <T extends Declaration> Iterable<T> declarations();

    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Declaration argmax = null;
        int max = Integer.MIN_VALUE;

        com.google.common.base.Function argmaxConverters[] = new com.google.common.base.Function[args.length];
        com.google.common.base.Function converters[] = new com.google.common.base.Function[args.length];
        int argMaxOffset = 0;

        for (Declaration method : declarations()) {
            MutableInt offset = new MutableInt(0);
            int score = score(method, args, converters, offset);
            if (score > max) {
                max = score;
                argmax = method;
                com.google.common.base.Function tmp[] = argmaxConverters;
                argMaxOffset = offset.intValue();
                argmaxConverters = converters;
                converters = tmp;
            }
        }

        if (argmax == null) {
            String context = "";
            if (thisObj instanceof JSBaseObject)
                context = " in an object of class " + JSBaseObject.getClassName(thisObj.getClass());

            throw ScriptRuntime.typeError(String.format("Could not find a matching method for %s(%s)%s",
                    getName(),
                    Output.toString(", ", args, o -> o.getClass().toString()),
                    context
            ));
        }

        // Call the constructor
        try {
            Object[] transformedArgs = transform(cx, scope, argmax, args, argmaxConverters, argMaxOffset);
            final Object result = argmax.invoke(transformedArgs);
            if (result == null) return  Undefined.instance;

            if (result instanceof JSBaseObject && !(result instanceof XPMObject.JSXPM))
                ((JSBaseObject) result).setXPM(XPMObject.getXPM(scope));
            return result;
        } catch (XPMRhinoException e) {
            throw e;
        } catch (Throwable e) {
            throw new WrappedException(new XPMRhinoException(e));
        }

    }

    /**
     * Transform the arguments
     *
     * @param cx
     * @param scope
     * @param declaration
     * @param args
     * @param offset The offset within the target parameters
     * @return
     */
    static Object[] transform(Context cx, Scriptable scope, Declaration declaration, Object[]
            args, Function[] converters, int offset) {
        final Executable executable = declaration.executable();
        final Class<?>[] types = executable.getParameterTypes();
        Object methodArgs[] = new Object[types.length];

        // --- Add context and scope if needed
        JSFunction annotation = executable.getAnnotation(JSFunction.class);

        final boolean useScope = annotation == null ? false : annotation.scope();
        if (useScope) {
            methodArgs[0] = cx;
            methodArgs[1] = scope;
        }

        // --- Copy the non vararg parameters
        final int length = types.length - (executable.isVarArgs() ? 1 : 0) - offset;
        int size = min(length, args.length);
        for (int i = 0; i < size; i++) {
            methodArgs[i + offset] = converters[i].apply(args[i]);
        }

        // --- Deals with the vararg pararameters
        if (executable.isVarArgs()) {
            final Class<?> varargType = types[types.length - 1].getComponentType();
            int nbVarargs = args.length - length;
            final Object array[] = (Object[]) Array.newInstance(varargType, nbVarargs);
            for (int i = 0; i < nbVarargs; i++) {
                array[i] = converters[i + length].apply(args[i + length]);
            }
            methodArgs[methodArgs.length - 1] = array;
        }

        return methodArgs;
    }

    static int score(Declaration declaration, Object[] args, Function[] converters, MutableInt offset) {

        final Executable executable = declaration.executable();
        final Class<?>[] types = executable.getParameterTypes();
        final boolean isVarArgs = executable.isVarArgs();

        // Get the annotations
        JSFunction annotation = declaration.executable.getAnnotation(JSFunction.class);
        final boolean scope = annotation == null ? false : annotation.scope();
        int optional = annotation == null ? 0 : annotation.optional();

        // Start the scoring
        Converter converter = new Converter();
        // Offset in the types
        offset.setValue(scope ? 2 : 0);

        // Number of "true" arguments (not scope, not vararg)
        final int nbArgs = types.length - offset.intValue() - (isVarArgs ? 1 : 0);

        // The number of arguments should be in:
        // [nbArgs - optional, ...] if varargs
        // [nbArgs - optional, nbArgs] otherwise

        if (args.length < nbArgs - optional)
            return Integer.MIN_VALUE;

        if (!isVarArgs && args.length > nbArgs)
            return Integer.MIN_VALUE;

        // If the optional arguments are at the beginning, then shift
        if (annotation != null && annotation.optionalsAtStart()) {
            offset.add(max(nbArgs - args.length, 0));
        }

        // Normal arguments
        for (int i = 0; i < args.length && i < nbArgs && converter.isOK(); i++) {
            final Object o = args[i];
            converters[i] = converter.converter(o, types[i + offset.intValue()]);
        }

        // Var args
        if (isVarArgs) {
            Class<?> type = ClassUtils.primitiveToWrapper(types[types.length - 1].getComponentType());
            int nbVarArgs = args.length - nbArgs;
            for (int i = 0; i < nbVarArgs && converter.isOK(); i++) {
                final Object o = args[nbArgs + i];
                converters[nbArgs + i] = converter.converter(o, type);
            }
        }

        return converter.score;
    }

    static public class ListConverter implements Function {
        private final Class<?> arrayClass;
        Function[] functions;

        public ListConverter(int size, Class<?> arrayClass) {
            this.arrayClass = arrayClass;
            this.functions = new Function[size];
        }

        void set(int index, Function function) {
            this.functions[index] = function;
        }

        @Override
        public Object apply(Object input) {
            final List list = (List)input;
            final Object[] objects = (Object[]) Array.newInstance(arrayClass, functions.length);
            for(int i = 0; i < objects.length; ++i) {
                objects[i] = functions[i].apply(list.get(i));
            }
            return objects;
        }
    }

    static public class Converter  {
        int score = Integer.MAX_VALUE;

        Function converter(Object o, Class<?> type) {
            if (o == null) {
                score--;
                return IDENTITY;
            }

            // Assignable: OK
            type = ClassUtils.primitiveToWrapper(type);
            if (type.isAssignableFrom(o.getClass())) {
                if (o.getClass() != type)
                    score--;
                return IDENTITY;
            }

            // Arrays
            if (o instanceof NativeArray && type.isArray()) {
                Class<?> innerType = type.getComponentType();
                final NativeArray array = (NativeArray) o;
                final ListConverter listConverter = new ListConverter(array.size(), innerType);
                for(int i = 0; i < array.getLength(); i++) {
                    listConverter.set(i, converter(array.get(i), innerType));
                    if (score == Integer.MIN_VALUE) {
                        return null;
                    }
                }
                return listConverter;
            }

            // Case of string: anything can be converted, but with different
            // scores
            if (type == String.class) {
                if (o instanceof Scriptable) {
                    switch (((Scriptable) o).getClassName()) {
                        case "String":
                        case "ConsString":
                            return Functions.toStringFunction();
                        default:
                            score -= 10;
                    }
                } else if (o instanceof CharSequence) {
                    score--;
                } else {
                    score -= 10;
                }
                return Functions.toStringFunction();
            }

            // Cast to integer
            if (type == Integer.class && o instanceof Number) {
                if ((((Number) o).intValue()) == ((Number) o).doubleValue()) {
                    return input -> ((Number) input).intValue();
                }
            }

            if (o instanceof Wrapper) {
                score -= 1;
                Function converter = converter(((Wrapper) o).unwrap(), type);
                return converter != null ? new Unwrapper(converter) : null;

            }
            score = Integer.MIN_VALUE;
            return null;
        }

        public boolean isOK() {
            return score != Integer.MIN_VALUE;
        }

    }

    static private final com.google.common.base.Function IDENTITY = Functions.identity();

    static private class Unwrapper implements com.google.common.base.Function {
        private final com.google.common.base.Function converter;

        public Unwrapper(com.google.common.base.Function converter) {
            this.converter = converter;
        }

        @Override
        public Object apply(Object input) {
            return converter.apply(((Wrapper) input).unwrap());
        }
    }

}
