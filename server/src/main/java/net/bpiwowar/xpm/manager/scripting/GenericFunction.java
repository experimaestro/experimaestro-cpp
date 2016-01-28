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
import org.mozilla.javascript.ScriptRuntime;
import net.bpiwowar.xpm.exceptions.WrappedException;
import net.bpiwowar.xpm.exceptions.XPMRhinoException;
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
    protected abstract <T extends Declaration> Iterable<T> declarations();

    /**
     * Call this method
     *
     * @param lcx     The language context
     * @param thisObj The object for which this method is called, or null if it is a static method
     * @param options The arguments passed as options
     * @param args    The arguments passed
     * @return
     */
    public Object call(LanguageContext lcx, Object thisObj, Map<String, Object> options, Object... args) {
        Converter argmax = null;

        Arguments arguments = new Arguments(lcx, args, options);

        for (int i = 0; i < args.length; ++i) {
            if (args[i] instanceof Wrapper) {
                args[i] = ((Wrapper) args[i]).unwrap();
            }
        }

        int n = 0;
        for (Declaration method : declarations()) {
            ++n;
            Converter converter = method.score(lcx, arguments, false);
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
                scoredDeclarations[i++] = declaration.score(lcx, arguments, true);
            }

            Arrays.sort(scoredDeclarations, (a, b) -> Integer.compare(a.score, b.score));


            final Logger logger = ScriptContext.mainLogger();
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
                final Logger logger = ScriptContext.get().getMainLogger();
                logger.warn("In %s", lcx.getScriptLocation());
                logger.warn("Method %s is deprecated", argmax.declaration);
                if (!deprecated.value().isEmpty()) {
                    logger.warn(deprecated.value());
                }
            }
            final Object result = argmax.declaration.invoke(lcx, thisObj, transformedArgs);

            return result;
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof XPMRhinoException) {
                throw (XPMRhinoException) e.getCause();
            }
            throw new WrappedException(new XPMRhinoException(e.getCause()));
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new WrappedException(new XPMRhinoException(e));
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
