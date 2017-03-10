package net.bpiwowar.xpm.manager.scripting;

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

import net.bpiwowar.xpm.exceptions.XPMScriptRuntimeException;
import net.bpiwowar.xpm.exceptions.WrappedException;
import net.bpiwowar.xpm.utils.Output;
import net.bpiwowar.xpm.utils.log.Logger;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;

/**
 * Base class for scripting methods or constructors
 */
public abstract class GenericFunction {

    /**
     * Get the name of the method or constructor
     */
    protected abstract Object getKey();

    /**
     * Give the list of declarations
     *
     * @param <T>
     * @return
     */
    public abstract <T extends Declaration> Iterable<T> declarations();

    /**
     * Call this method
     *
     * @param thisObj The object for which this method is called, or null if it is a static method
     * @param options The arguments passed as options
     * @param args    The arguments passed
     * @return
     */
    public Object call(Object thisObj, Map<String, Object> options, Object... args) {
        Converter argmax = null;

        Arguments arguments = new Arguments(args, options);

        for (int i = 0; i < args.length; ++i) {
            if (args[i] instanceof Wrapper) {
                args[i] = ((Wrapper) args[i]).unwrap();
            }
        }

        int n = 0;
        for (Declaration method : declarations()) {
            ++n;
            Converter converter = method.score(arguments, false);
            if (converter.score > 0  && (argmax == null  || converter.score > argmax.score)) {
                argmax = converter;
            }
        }

        // No method matched... we try to help the user by displaying available ones
        if (argmax == null) {
            String context = "";

            // Print the best matching methods
            Converter scoredDeclarations[] = new Converter[n];
            int i = 0;
            for (Declaration declaration : declarations()) {
                scoredDeclarations[i++] = declaration.score(arguments, true);
            }

            Arrays.sort(scoredDeclarations, Comparator.comparingInt(a -> a.score));


            final Logger logger = Context.mainLogger();
            final String message = String.format("Could not find a matching method for %s(%s)%s",
                    getKey(),
                    Output.toString(", ", args, o -> o.getClass().toString()),
                    context
            );

            logger.error(message);
            logger.error("Candidates are:");
            for (Converter scoredDeclaration : scoredDeclarations) {
                logger.error("[%d] %s", scoredDeclaration.score, scoredDeclaration.declaration);
            }

            throw new XPMScriptRuntimeException(message);
        }


        // Call the function
        try {
            Object[] transformedArgs = argmax.transform(arguments);
            // Show deprecated methods
            final Deprecated deprecated = argmax.declaration.executable().getAnnotation(Deprecated.class);
            if (deprecated != null) {
                final Logger logger = Context.get().getMainLogger();
                logger.warn("Method %s is deprecated", argmax.declaration);
                if (!deprecated.value().isEmpty()) {
                    logger.warn(deprecated.value());
                }
            }

            return argmax.declaration.invoke(thisObj, transformedArgs);
        } catch (InvocationTargetException e) {
            throw new WrappedException(new XPMScriptRuntimeException(e.getCause()));
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new WrappedException(new XPMScriptRuntimeException(e));
        }

    }


    static public final class ArgumentConverter implements Function<Arguments, Object> {
        private final int position;

        public ArgumentConverter(int position) {
            this.position = position;
        }

        @Override
        public Object apply(Arguments arguments) {
            return arguments.args[position];
        }
    }


    static public class ListConverter implements Function<Object, Object> {
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

}
