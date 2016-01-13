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

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import net.bpiwowar.xpm.manager.js.JSBaseObject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;

/**
 * Automated javascript documentation
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Documentation {
    static void documentMethod(net.bpiwowar.xpm.utils.Documentation.DefinitionList methods, Method method) {
        final net.bpiwowar.xpm.utils.Documentation.Text text = methodDeclaration(method);

        // Get the help
        net.bpiwowar.xpm.utils.Documentation.Container help = new net.bpiwowar.xpm.utils.Documentation.Container();
        final Help jsHelp = method.getAnnotation(Help.class);

        if (jsHelp != null)
            help.add(new net.bpiwowar.xpm.utils.Documentation.Text(jsHelp.value()));

        methods.add(text, help);
    }

    public static net.bpiwowar.xpm.utils.Documentation.Text methodDeclaration(Method method) {
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


        final net.bpiwowar.xpm.utils.Documentation.DefinitionList pHelp = new net.bpiwowar.xpm.utils.Documentation.DefinitionList();

        // We have the arguments() in the JSHelp() object
        // No JSHelp
        final net.bpiwowar.xpm.utils.Documentation.Text text = new net.bpiwowar.xpm.utils.Documentation.Text();
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
                        pHelp.add(new net.bpiwowar.xpm.utils.Documentation.Text(jsArg.name()), new net.bpiwowar.xpm.utils.Documentation.Text(jsArg.help()));
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
        if (JSBaseObject.class.isAssignableFrom(aClass)) {
            return ClassDescription.getClassName(aClass);
        }

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
    static public void printJSHelp(net.bpiwowar.xpm.utils.Documentation.Printer printer) {

        // --- Document functions

        printer.append(new net.bpiwowar.xpm.utils.Documentation.Title(1, new net.bpiwowar.xpm.utils.Documentation.Text("Functions")));

        final net.bpiwowar.xpm.utils.Documentation.DefinitionList functions = new net.bpiwowar.xpm.utils.Documentation.DefinitionList();

//        for (JSUtils.FunctionDefinition d : JavaScriptRunner.definitions) {
//            final Documentation.Text text = new Documentation.Text(d.getName());
//            try {
//                final Method method = d.getClazz().getDeclaredMethod("js_" + d.getName(), d.getArguments());
//                documentMethod(functions, method, d.getName());
//            } catch (NoSuchMethodException e) {
//                text.format("Method not found... %s in [%s] with %s", d.getName(), d.getClazz().toString(), Arrays.toString(d.getArguments()));
//            }
//        }

        printer.append(functions);


        // --- Objects
        printer.append(new net.bpiwowar.xpm.utils.Documentation.Title(1, new net.bpiwowar.xpm.utils.Documentation.Text("Objects")));
        ArrayList<Class<?>> list = new ArrayList<>();

        final net.bpiwowar.xpm.utils.Documentation.DefinitionList classes = new net.bpiwowar.xpm.utils.Documentation.DefinitionList();

        for (Class<?> clazz : list) {
            final net.bpiwowar.xpm.utils.Documentation.DefinitionList methods = new net.bpiwowar.xpm.utils.Documentation.DefinitionList();

            for (Method method : clazz.getDeclaredMethods()) {
                String name = null;

                // A javascript object based on JSBaseObject
                if (JSBaseObject.class.isAssignableFrom(clazz)) {
                    Expose annotation = method.getAnnotation(Expose.class);
                    if (annotation != null)
                        documentMethod(methods, method);
                    continue;
                }

                if (ScriptableObject.class.isAssignableFrom(clazz)) {
                    if (method.getAnnotation(Expose.class) != null) {
                        if (method.getName().startsWith("js_")) {
                            name = method.getName();
                        } else {
                            continue;
                        }
                    }
                } else {
                    if (!Modifier.isPublic(method.getModifiers()))
                        continue;
                    name = method.getName();
                }

                documentMethod(methods, method);
            }
            classes.add(new net.bpiwowar.xpm.utils.Documentation.Text(ClassDescription.getClassName(clazz)), methods);

        }

        printer.append(classes);

    }
}
