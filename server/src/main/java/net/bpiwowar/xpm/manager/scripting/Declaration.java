package net.bpiwowar.xpm.manager.scripting;

import org.apache.commons.lang.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Reflection for a method or a constructor that can be called from a scripting language
 *
 * @param <T> The type of executable (constructor or method)
 */
abstract public class Declaration<T extends Executable> {
    /**
     * The underlying executable
     */
    private final T executable;

    private Expose expose;

    private int nbArguments;

    /**
     * Index of the argument marked with options
     */
    private int optionsIndex = -1;

    boolean[] nullable;

    String[] parameterNames;
    String name;

    /**
     * Construct a new declaration
     *
     * @param executable The executable to wrap
     */
    public Declaration(T executable) {
        this.executable = executable;

        // Get the name
        Expose expose = executable.getAnnotation(Expose.class);
        name = expose != null && !expose.value().isEmpty() ? expose.value()
                : (executable instanceof Constructor ? null : executable.getName());
    }

    private static <T extends Annotation> T getAnnotation(Class<T> annotationClass, Annotation[] annnotations) {
        final Optional<Annotation> first = Arrays.stream(annnotations)
                .filter(x -> annotationClass.isAssignableFrom(x.getClass())).findFirst();
        if (first.isPresent()) {
            return (T) first.get();
        }
        return null;
    }


    /**
     * Returns the executable
     */
    public Executable executable() {
        return executable;
    }

    public abstract Object invoke(Object thisObj, Object[] transformedArgs) throws InvocationTargetException, IllegalAccessException, InstantiationException;


    @Expose
    static private void exposemethod() {
    }

    static private Expose DEFAULT_EXPOSE;

    {
        try {
            DEFAULT_EXPOSE = Declaration.class.getDeclaredMethod("exposemethod").getAnnotation(Expose.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Compute method structure
     *
     * @return
     */
    final Declaration<T> init() {
        if (expose == null) {
            // Get the annotations
            expose = executable.getAnnotation(Expose.class);
            if (expose == null) {
                expose = DEFAULT_EXPOSE;
            }

            final Class<?>[] types = executable.getParameterTypes();

            nullable = new boolean[types.length];

            nbArguments = types.length - (executable.isVarArgs() ? 1 : 0) - expose.optional();
            final Annotation[][] annotations = executable.getParameterAnnotations();

            // Look at parameters
            for (int i = 0; i < types.length; ++i) {
                nullable[i] = true;

                for (Annotation a : annotations[i]) {
                    if (a instanceof Options) {
                        if (Stream.of(annotations[i]).anyMatch(c -> c instanceof Options)) {
                            if (optionsIndex >= 0) {
                                expose = null;
                                throw new AssertionError("Only one parameter can be annotated with @Options");
                            }
                            optionsIndex = i;
                        }
                    } else if (a instanceof NotNull) {
                        nullable[i] = false;
                    }
                }
            }

        }
        return this;
    }

    boolean nullable(int i) {
        return nullable[i];
    }

    public Type[] getGenericParameterTypes() {
        return executable.getGenericParameterTypes();
    }

    public Annotation[][] getParameterAnnotations() {
        return executable.getParameterAnnotations();
    }

    public int getParameterCount() {
        return executable.getParameterCount();
    }

    public String[] getParameterNames() {
        if (parameterNames == null) {
            Set<String> set = new HashSet<>();

            parameterNames = new String[getParameterCount()];
            Parameter[] parameters = executable.getParameters();

            for (int i = 0; i < parameterNames.length; ++i) {
                String s = parameterNames[i] = name(parameters[i], set);
                set.add(s);
            }
        }
        return parameterNames;
    }

    static private final HashSet<String> RESERVED_CPP_NAMES = new HashSet<>();

    static {
        RESERVED_CPP_NAMES.add("int");
        RESERVED_CPP_NAMES.add("long");
        RESERVED_CPP_NAMES.add("float");
        RESERVED_CPP_NAMES.add("float");
        RESERVED_CPP_NAMES.add("json");
    }

    static private String name(Parameter p, Set<String> taken) {
        final Argument arg = p.getAnnotation(Argument.class);
        if (arg != null) {
            return arg.name();
        }

        if (!p.getName().startsWith("arg"))
            return p.getName();

        Class<?> type = p.getType();
        if (type.isArray())
            type = type.getComponentType();

        String base = type.getSimpleName();
        base = base.substring(0, 1).toLowerCase() + base.substring(1);
        String proposed = base;

        int count = 0;
        while (taken.contains(proposed) || RESERVED_CPP_NAMES.contains(proposed)) {
            proposed = base + "_" + (++count);
        }
        return proposed;
    }

    public Type getReturnType() {
        return executable.getAnnotatedReturnType().getType();
    }

    public boolean isStatic() {
        return executable instanceof Constructor || Modifier.isStatic(executable.getModifiers());
    }

    public String getName() {
        return name;
    }

    public boolean isPureVirtual() {
        return executable.getDeclaringClass().isInterface()
                && !((Method) executable).isDefault();
    }

    /**
     * Gives a score to a given declaration
     *
     * @param arguments The arguments
     * @param fullCheck If all arguments should be checked (even if the method does not match)
     * @return An arguments converter, containing a score and a list of converter functions
     */
    public Converter score(Arguments arguments, boolean fullCheck) {
        init();

        final Class<?>[] types = executable.getParameterTypes();
        final boolean isVarArgs = executable.isVarArgs();
        final Object[] args = arguments.args;

        // Start the scoring
        Converter converter = new Converter(this);
        converter.functions = new Function[types.length];

        // Current position in the arguments
        int position = 0;

        Function<Integer, Integer> nextPosition = x -> x + 1;


        // Get the number of needed argument, that the number of arguments in the Java declaration
        // -1 if varargs
        // -1 if one of the argument is options
        // -n if n arguments are optional
        int neededArgs = nbArguments;

        // If we have named arguments, then check if the declaration supports this
        if (arguments.options != null && !arguments.options.isEmpty()) {
            if (optionsIndex < 0) {
                return converter.invalidate();
            }

            // one less needed argument
            --neededArgs;
            final int ix = optionsIndex;
            nextPosition = x -> {
                ++x;
                if (x == ix) {
                    ++x;
                }
                return x;
            };

            // Process options
            Object o = arguments.options;
            final Function<Object, Object> f = converter.converter(o, types[ix], false);
            if (!converter.isOK() && !fullCheck) {
                return converter;
            }

            converter.functions[ix] = f.compose(a -> a.options);
        }

        // The number of arguments should be in:
        // [neededArgs, ...] if varargs
        // [neededArgs, neededArgs + optionals] otherwise
        if (args.length < neededArgs) {
            return converter.invalidate();
        }

        if (!isVarArgs && args.length > neededArgs + expose.optional()) {
            return converter.invalidate();
        }

        // Comute the number of args which are not varargs
        int nbArgs = Integer.min(neededArgs + expose.optional(), args.length);

        // If the optional arguments are at the beginning, then shift
        if (expose.optionalsAtStart()) {
            position = nextPosition.apply(position + Integer.max(neededArgs + expose.optional() - args.length, 0) - 1);
        }

        // Normal arguments
        for (int i = 0; i < args.length && i < nbArgs && (converter.isOK() || fullCheck); i++) {
            Object o = args[i];

            final Function<Object, Object> f = converter.converter(o, types[position], nullable(position));
            if (f != null) {
                converter.functions[position] = f.compose(new GenericFunction.ArgumentConverter(i));
            }

            position = nextPosition.apply(position);
        }


        final Annotation[][] annotations = executable.getParameterAnnotations();

        // Process varargs
        if (isVarArgs) {
            final int lastix = types.length - 1;
            final Argument argument = getAnnotation(Argument.class, annotations[lastix]);
            final boolean nullable = nullable(lastix);

            // Compute the argument type or get it from annotations
            Class<?>[] argTypes;
            if (argument != null && argument.types().length > 0) {
                // From annotation
                argTypes = argument.types();
            } else {
                // From the varargs argument
                argTypes = new Class<?>[]{types[lastix].getComponentType()};
            }


            for (int i = 0; i < argTypes.length; ++i) {
                argTypes[i] = ClassUtils.primitiveToWrapper(argTypes[i]);
            }

            int nbVarArgs = args.length - nbArgs;

            final Converter.VarArgsConverter varArgsConverter = new Converter.VarArgsConverter(types[lastix].getComponentType());

            for (int i = 0; i < nbVarArgs && converter.isOK(); i++) {
                final int j = nbArgs + i;
                final Object o = args[j];

                int bestScore = 0;
                int oldScore = converter.score;

                // Find the best among the k
                Function<Object, Object> best = null;
                for (int k = 0; k < argTypes.length; ++k) {
                    converter.score = oldScore;
                    Function<Object, Object> f = converter.converter(o, argTypes[k], nullable);
                    if (k == 0 || converter.score > bestScore) {
                        best = f;
                        bestScore = converter.score;
                    } else {
                        converter.score = bestScore;
                    }
                }

                // Add the converter
                if (best != null) {
                    varArgsConverter.add(best.compose(new GenericFunction.ArgumentConverter(j)));
                } else {
                    varArgsConverter.add(null);
                }
            }

            converter.functions[lastix] = varArgsConverter;
        }

        return converter;
    }

}
