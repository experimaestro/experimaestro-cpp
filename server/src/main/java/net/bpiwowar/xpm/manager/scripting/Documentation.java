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

import net.bpiwowar.xpm.documentation.*;
import net.bpiwowar.xpm.documentation.Documentation.Printer;
import net.bpiwowar.xpm.utils.log.Logger;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Automated javascript documentation
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Documentation {
    final static Logger LOGGER = Logger.getLogger();

    static void documentMethod(DefinitionList methods, Method method) {
        final Text text = methodDeclaration(method);

        // Get the help
        Container help = new Container();
        final Help jsHelp = method.getAnnotation(Help.class);

        if (jsHelp != null)
            help.add(new Text(jsHelp.value()));

        methods.add(text, help);
    }

    public static Text methodDeclaration(Method method) {
        String name = null;

        Expose xpmjsfunction = method.getAnnotation(Expose.class);
        if (xpmjsfunction != null) {
            if (!xpmjsfunction.value().equals(""))
                name = xpmjsfunction.value();
        }

        Expose jsfunction = method.getAnnotation(Expose.class);
        if (name == null && jsfunction != null) {
            name = jsfunction.value();
            if (name.equals(""))
                name = method.getName();
        }


        if (name == null) {
            name = method.getName();
        }

        String prefix = "";
        if (method.getAnnotation(Deprecated.class) != null) {
            prefix = "[deprecated]";
        }


        final DefinitionList pHelp = new DefinitionList();

        // We have the arguments() in the JSHelp() object
        // No JSHelp
        final Text text = new Text();
        final Argument returnAnnotation = method.getAnnotation(Argument.class);
        String returnType = returnAnnotation != null ? returnAnnotation.type() : javascriptName(method.getReturnType());

        text.format("%s%s %s(", prefix, returnType, name);

        int startAt = 0;
        if (jsfunction != null && jsfunction.context())
            startAt = 1;

        Parameter[] parameters = method.getParameters();
        boolean first = true;
        for (int i = startAt; i < parameters.length; i++) {
            String argName = parameters[i].getName();
            String argType = null;

            for (Annotation a : parameters[i].getAnnotations()) {
                if (a instanceof Argument) {
                    final Argument jsArg = (Argument) a;
                    argName = jsArg.equals("") ? argName : jsArg.name();
                    argType = jsArg.type();
                    if (jsArg.help() != null)
                        pHelp.add(new Text(jsArg.name()), new Text(jsArg.help()));
                }
            }
            if (!first)
                text.append(", ");
            else
                first = false;

            if (argType == null || "".equals(argType)) {
                Class<?> pClass = parameters[i].getType();
                argType = javascriptName(pClass);
            }

            if (argName.equals("")) argName = "arg_" + i;
            text.format("%s %s", argType, argName);
        }
        text.append(")");
        return text;
    }

    private static String javascriptName(Class<?> aClass) {
        if (aClass.isArray())
            return javascriptName(aClass.getComponentType()) + "[]";

        if (aClass.isPrimitive() || aClass == String.class || aClass == Boolean.class
                || aClass == Integer.class || aClass == Long.class || aClass == Object.class
                || aClass == String.class)
            return aClass.getSimpleName();

        if (aClass == NativeArray.class)
            return "Array";

        if (aClass == Scriptable.class)
            return "Object";

        return "NA/" + aClass.getCanonicalName();
    }

    /**
     * Retrieves by introspection the functions and objects defined
     * and prints a documentation out of it
     *
     * @param printer
     */
    static public void printHelp(Printer printer) {
        try {
            // --- Document functions

            // Add constants

            printer.write(new Title(1, new Text("Constants")));

            Scripting.forEachConstant((name, value) -> {
                printer.write(new Division().add(new Text(name)));
            });

            Scripting.forEachObject((name, value) -> {
                        printer.write(new Division().add(new Text(name)));
                    }
            );


            // Add functions
            printer.write(new Title(1, new Text("Functions")));
            DefinitionList functions = new DefinitionList();
            Scripting.forEachFunction(list -> {
                list.groups.stream()
                        .flatMap(g -> g.getMethods().stream())
                        .forEach(method -> documentMethod(functions, method));
            });
            printer.write(functions);


            // --- Objects
            printer.write(new Title(1, new Text("Classes")));

            DefinitionList classes = new DefinitionList();

            Scripting.forEachType(aClass -> {
                ClassDescription type = ClassDescription.analyzeClass(aClass);
                DefinitionList list = new DefinitionList();
                classes.add(new Text(type.getClassName()), list);
                type.getMethods().forEach((name, methods) -> {
//                    methods.for documentMethod(list, methods);
                });
            });
            printer.write(classes);

        } catch (Throwable e) {
            LOGGER.error(e, "Error while generating help");
            throw e;
        }
    }
}
