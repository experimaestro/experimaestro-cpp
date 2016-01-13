package net.bpiwowar.xpm.manager.scripting;

import org.apache.commons.lang.ClassUtils;
import net.bpiwowar.xpm.manager.json.Json;
import net.bpiwowar.xpm.scheduler.Resource;
import net.bpiwowar.xpm.utils.arrays.ListAdaptator;

import java.lang.reflect.Array;
import java.lang.reflect.Executable;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

/**
 * A converter
 */
public class Converter {

    public static final int NON_MATCHING_COST = 1000;
    static private final Function<Object, Object> IDENTITY = Function.identity();
    static private final Function<Object, Object> TOSTRING = x -> x.toString();

    final Declaration declaration;
    int score = Integer.MAX_VALUE;
    public Function<Arguments, Object>[] functions;

    public <T extends Executable> Converter(Declaration declaration) {
        this.declaration = declaration;
    }

    /**
     * Returns a function that convert the object into a given type
     *
     * @param lcx      The scripting language context
     * @param object   The object to convert
     * @param type     The type of the target argument
     * @param nullable Whether the value can be null
     * @return
     */
    Function<Object, Object> converter(LanguageContext lcx, Object object, Class<?> type, boolean nullable) {
        if (object == null) {
            if (!nullable) {
                return nonMatching();
            }
            if (type.isPrimitive()) {
                return nonMatching();
            }
            score--;
            return IDENTITY;
        }

        // Assignable: OK
        type = ClassUtils.primitiveToWrapper(type);
        if (type.isAssignableFrom(object.getClass())) {
            if (object.getClass() != type)
                score--;
            return IDENTITY;
        }

        // Arrays
        if (type.isArray()) {
            Class<?> innerType = type.getComponentType();

            if (object.getClass().isArray())
                object = ListAdaptator.create((Object[]) object);

            if (object instanceof Collection) {
                final Collection array = (Collection) object;
                final Iterator iterator = array.iterator();
                final GenericFunction.ListConverter listConverter = new GenericFunction.ListConverter(innerType);

                while (iterator.hasNext()) {
                    listConverter.add(converter(lcx, iterator.next(), innerType, true));
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
            if (object instanceof CharSequence) {
                score--;
            } else {
                score -= 10;
            }
            return TOSTRING;
        }

        // Cast to integer
        if (type == Integer.class && object instanceof Number) {
            if ((((Number) object).intValue()) == ((Number) object).doubleValue()) {
                return input -> ((Number) input).intValue();
            }
        }

        // JSON inputs
        if (Json.class.isAssignableFrom(type)) {
            if (object instanceof Map
                    || object instanceof List || object instanceof Double || object instanceof Float
                    || object instanceof Integer || object instanceof Long
                    || object instanceof Path || object instanceof Boolean
                    || object instanceof Resource || object instanceof ScriptingPath
                    || object instanceof BigInteger) {
                score -= 10;
                return x -> Json.toJSON(lcx, x);
            }
        }

        return nonMatching();
    }

    private Function nonMatching() {
        score = score > 0 ? 0 : score - NON_MATCHING_COST;
        return null;
    }

    public boolean isOK() {
        return score > 0;
    }

    public Converter invalidate() {
        score = Integer.MIN_VALUE;
        return this;
    }


    /**
     * Transform the arguments
     *
     * @param arguments The method arguments
     * @return The transformed arguments
     */
    Object[] transform(Arguments arguments) {
        Object methodArgs[] = new Object[functions.length];

        for (int i = 0; i < functions.length; ++i) {
            if (functions[i] != null) {
                methodArgs[i] = functions[i].apply(arguments);
            }
        }

        return methodArgs;
    }

    static public class VarArgsConverter implements Function<Arguments, Object> {
        private final Class<?> type;
        ArrayList<Function<Arguments, Object>> list = new ArrayList<>();

        public VarArgsConverter(Class<?> type) {
            this.type = type;
        }

        @Override
        public Object apply(Arguments arguments) {
            final Object array = Array.newInstance(this.type, list.size());
            for (int i = 0; i < list.size(); ++i) {
                if (list.get(i) != null) {
                    Array.set(array, i, list.get(i).apply(arguments));
                }
            }
            return array;
        }

        public void add(Function<Arguments, Object> function) {
            list.add(function);
        }
    }
}
