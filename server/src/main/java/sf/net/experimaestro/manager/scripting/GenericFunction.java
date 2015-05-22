package sf.net.experimaestro.manager.scripting;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
 *
 * experimaestro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * experimaestro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.mutable.MutableInt;
import org.mozilla.javascript.ScriptRuntime;
import sf.net.experimaestro.exceptions.WrappedException;
import sf.net.experimaestro.exceptions.XPMRhinoException;
import sf.net.experimaestro.manager.js.JSBaseObject;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonObject;
import sf.net.experimaestro.manager.json.JsonReal;
import sf.net.experimaestro.utils.Output;
import sf.net.experimaestro.utils.arrays.ListAdaptator;
import sf.net.experimaestro.utils.log.Logger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.Math.max;
import static java.lang.StrictMath.min;

/**
 * Base class for scripting methods or constructors
 */
public abstract class GenericFunction {

    static private final Function IDENTITY = Function.identity();
    static private final Function<Wrapper, Object> UNWRAPPER = x -> x.unwrap();
    static private final Function<org.mozilla.javascript.Wrapper, Object> JS_UNWRAPPER = x -> x.unwrap();
    static private final Function<Object, String> TOSTRING = x -> x.toString();

    /**
     * Transform the arguments
     *
     * @param lcx
     * @param declaration
     * @param args
     * @param offset      The offset within the target parameters
     * @return The transformed arguments
     */
    static Object[] transform(LanguageContext lcx, Declaration declaration, Object[]
            args, Function[] converters, int offset) {
        final Executable executable = declaration.executable();
        final Class<?>[] types = executable.getParameterTypes();
        Object methodArgs[] = new Object[types.length];

        // --- Add context and scope if needed
        Expose annotation = executable.getAnnotation(Expose.class);

        if (annotation == null ? false : annotation.context()) {
            methodArgs[0] = lcx;
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

    /**
     * Gives a score to a given declaration
     *
     * @param declaration The underlying method or constructor
     * @param args        The arguments
     * @param converters  A list of converters that will be filled by this method
     * @param offset      The offset for the converters
     * @param full
     * @return A score (minimum integer if no conversion is possible)
     */
    static int score(LanguageContext lcx, Declaration declaration, Object[] args, Function[] converters, MutableInt offset, boolean full) {

        final Executable executable = declaration.executable();
        final Class<?>[] types = executable.getParameterTypes();
        final boolean isVarArgs = executable.isVarArgs();
        final Annotation[][] annotations = executable.getParameterAnnotations();

        // Get the annotations
        Expose annotation = declaration.executable.getAnnotation(Expose.class);
        final boolean contextAnnotation = annotation == null ? false : annotation.context();
        int optional = annotation == null ? 0 : annotation.optional();

        // Start the scoring
        Converter converter = new Converter();

        // Offset in the types
        offset.setValue(contextAnnotation ? 1 : 0);

        // Number of "true" arguments (not scope, not vararg)
        final int nbArgs = types.length - offset.intValue() - (isVarArgs ? 1 : 0);

        // The number of arguments should be in:
        // [nbArgs - optional, ...] if varargs
        // [nbArgs - optional, nbArgs] otherwise

        if (args.length < nbArgs - optional) {
            return Integer.MIN_VALUE;
        }

        if (!isVarArgs && args.length > nbArgs) {
            return Integer.MIN_VALUE;
        }

        // If the optional arguments are at the beginning, then shift
        if (annotation != null && annotation.optionalsAtStart()) {
            offset.add(max(nbArgs - args.length, 0));
        }

        // Normal arguments
        for (int i = 0; i < args.length && i < nbArgs && (converter.isOK() || full); i++) {
            final int j = i + offset.intValue();

            Object o = args[i];

            // Unwrap if necessary
            final boolean javaize = Stream.of(annotations[j]).noneMatch(c -> c instanceof NoJavaization);
            if (javaize) {
                o = lcx.toJava(o);
            }

            converters[i] = converter.converter(lcx, o, types[j]);
            if (javaize && converters[i] != null) {
                converters[i] = converters[i].compose(lcx::toJava);
            }
        }

        // Var args
        if (isVarArgs) {
            Class<?> type = ClassUtils.primitiveToWrapper(types[types.length - 1].getComponentType());
            int nbVarArgs = args.length - nbArgs;
            for (int i = 0; i < nbVarArgs && converter.isOK(); i++) {
                final int j = nbArgs + i;
                final Object o = lcx.toJava(args[(j)]);
                converters[j] = converter.converter(lcx, o, type);
                if (converters[j] != null) {
                    converters[j] = converters[j].compose(lcx::toJava);
                }
            }
        }

        return converter.score;
    }

    /**
     * Get the name of the method or constructor
     */
    protected abstract Object getKey();

    protected abstract <T extends Declaration> Iterable<T> declarations();

    public Object call(LanguageContext lcx, Object thisObj, Object... args) {
        Declaration argmax = null;
        int max = 0;

        Function argmaxConverters[] = new Function[args.length];
        Function converters[] = new Function[args.length];
        int argMaxOffset = 0;

        for(int i = 0; i < args.length; ++i) {
            if (args[i] instanceof Wrapper) {
                args[i] = ((Wrapper)args[i]).unwrap();
            }
        }

        int n = 0;
        for (Declaration method : declarations()) {
            ++n;

            MutableInt offset = new MutableInt(0);
            int score = score(lcx, method, args, converters, offset, false);
            if (score > max) {
                max = score;
                argmax = method;
                Function tmp[] = argmaxConverters;
                argMaxOffset = offset.intValue();
                argmaxConverters = converters;
                converters = tmp;
            }
        }

        if (argmax == null) {
            String context = "";
            if (thisObj instanceof JSBaseObject) {
                context = " in an object of class " + ((JSBaseObject) thisObj).getClassName();
            }

            // Print the best matching methods
            ScoredDeclaration scoredDeclarations[] = new ScoredDeclaration[n];
            int i = 0;
            for (Declaration declaration : declarations()) {
                MutableInt offset = new MutableInt(0);
                scoredDeclarations[i++] = new ScoredDeclaration(declaration,score(lcx, declaration, args, converters, offset, true));
            }

            Arrays.sort(scoredDeclarations, (a, b) -> Integer.compare(a.score, b.score));


            final Logger logger = ScriptContext.get().getLogger("xpm");
            final String message = String.format("Could not find a matching method for %s(%s)%s",
                    getKey(),
                    Output.toString(", ", args, o -> o.getClass().toString()),
                    context
            );

            logger.error(message);
            logger.error("Candidates are:");
            for(ScoredDeclaration scoredDeclaration: scoredDeclarations) {
                logger.error("%s", scoredDeclaration.method);
            }
            throw ScriptRuntime.typeError(message);
        }

        // Call the constructor
        try {
            Object[] transformedArgs = transform(lcx, argmax, args, argmaxConverters, argMaxOffset);
            if (argmax.executable.getAnnotation(Deprecated.class) != null) {
                ScriptContext.get().getLogger("xpm").warn("Method %s is deprecated", argmax);
            }
            final Object result = argmax.invoke(lcx, transformedArgs);

            return result;
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof XPMRhinoException) {
                throw (XPMRhinoException) e.getCause();
            }
            throw new WrappedException(new XPMRhinoException(e.getCause()));
        } catch(RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new WrappedException(new XPMRhinoException(e));
        }

    }

    abstract static public class Declaration<T extends Executable> {
        private final T executable;

        public Declaration(T executable) {
            this.executable = executable;
        }

        public Executable executable() {
            return executable;
        }

        public abstract Object invoke(LanguageContext cx, Object[] transformedArgs) throws InvocationTargetException, IllegalAccessException, InstantiationException;

    }

    static public class ListConverter implements Function {
        private final Class<?> arrayClass;
        ArrayList<Function> functions = new ArrayList<>();

        public ListConverter(Class<?> arrayClass) {
            this.arrayClass = arrayClass;
        }

        @Override
        public Object apply(Object input) {
            final Collection collection = (Collection) input;
            final Object[] objects = (Object[]) Array.newInstance(arrayClass, functions.size());
            final Iterator iterator = collection.iterator();
            int i = 0;
            while (iterator.hasNext()) {
                objects[i] = functions.get(i).apply(iterator.next());
                ++i;
            }
            assert i == objects.length;
            return objects;
        }

        public void add(Function function) {
            functions.add(function);
        }
    }

    /**
     * A converter
     */
    static public class Converter {

        public static final int NON_MATCHING_COST = 1000;

        int score = Integer.MAX_VALUE;

        Function converter(LanguageContext lcx, Object o, Class<?> type) {
            if (o == null) {
                if (type.isPrimitive()) {
                    score = score > 0 ? 0 : score - NON_MATCHING_COST;
                    return null;
                }
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
            if (type.isArray()) {
                Class<?> innerType = type.getComponentType();

                if (o.getClass().isArray())
                    o = ListAdaptator.create((Object[]) o);

                if (o instanceof Collection) {
                    final Collection array = (Collection) o;
                    final Iterator iterator = array.iterator();
                    final ListConverter listConverter = new ListConverter(innerType);

                    while (iterator.hasNext()) {
                        listConverter.add(converter(lcx, iterator.next(), innerType));
                        if (score == Integer.MIN_VALUE) {
                            return null;
                        }
                    }

                    return listConverter;

                }
            }

            // Case of string: anything can be converted, but with different
            // scores
            if (type == String.class) {
                if (o instanceof CharSequence) {
                    score--;
                } else {
                    score -= 10;
                }
                return TOSTRING;
            }

            // Cast to integer
            if (type == Integer.class && o instanceof Number) {
                if ((((Number) o).intValue()) == ((Number) o).doubleValue()) {
                    return input -> ((Number) input).intValue();
                }
            }

            // JSON inputs
            if (Json.class.isAssignableFrom(type)) {
                if (o instanceof Map
                        || o instanceof List || o instanceof Double || o instanceof Float
                        || o instanceof Integer || o instanceof Long) {
                    score -= 10;
                    return x -> Json.toJSON(lcx, x);
                }
            }

            score = score > 0 ? 0 : score - NON_MATCHING_COST;
            return null;
        }

        public boolean isOK() {
            return score > 0;
        }

    }

    static private class ScoredDeclaration {
        private final Declaration method;

        private final int score;

        public ScoredDeclaration(Declaration method, int score) {
            this.method = method;
            this.score = score;
        }
    }
}
