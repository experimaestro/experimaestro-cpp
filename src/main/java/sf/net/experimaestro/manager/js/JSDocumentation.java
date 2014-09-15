package sf.net.experimaestro.manager.js;

import bpiwowar.argparser.utils.Introspection;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import sf.net.experimaestro.utils.Documentation;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.Output;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;

import static java.lang.String.format;

/**
 * Automated javascript documentation
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 16/1/13
 */
public class JSDocumentation {
    static void documentMethod(Documentation.DefinitionList methods, Method method, String name) {
        sf.net.experimaestro.manager.js.JSFunction xpmjsfunction = method.getAnnotation(sf.net.experimaestro.manager.js.JSFunction.class);
        if (xpmjsfunction != null) {
            if (!xpmjsfunction.value().equals(""))
                name = xpmjsfunction.value();
        }

        JSFunction jsfunction = method.getAnnotation(JSFunction.class);
        if (name == null && jsfunction != null) {
            name = jsfunction.value();
            if (name.equals(""))
                name = method.getName();
        }


        if (name == null) {
            final org.mozilla.javascript.annotations.JSFunction annotation = method.getAnnotation(org.mozilla.javascript.annotations.JSFunction.class);
            if (annotation != null)
                name = annotation.value();
        }


        if (name == null) {
            name = method.getName();
        }

        String prefix = "";
        if (method.getAnnotation(JSDeprecated.class) != null) {
            prefix = "[deprecated]";
        }

        Documentation.Container help = new Documentation.Container();
        final JSHelp jsHelp = method.getAnnotation(JSHelp.class);

        if (jsHelp != null)
            help.add(new Documentation.Text(jsHelp.value()));

        Documentation.Content names = null;
        final Documentation.DefinitionList pHelp = new Documentation.DefinitionList();

        // We have the arguments() in the JSHelp() object
        if (jsHelp != null && jsHelp.arguments().length > 0) {
            final Documentation.Container container = new Documentation.Container();
            names = container;

            for (JSArguments arguments : jsHelp.arguments()) {
                final String s = Output.toString(", ", arguments.value(), o -> {
                    if (o.help() != null) {
                        pHelp.add(new Documentation.Text(o.name()), new Documentation.Text(o.help()));
                    }
                    return format("%s %s", o.type(), o.name());
                });
                String returnType = arguments.returnType();
                if ("".equals(returnType))
                    returnType = javascriptName(method.getReturnType());
                container.add(new Documentation.Division(
                        new Documentation.Text(format("%s%s %s(%s)\n", prefix, returnType, name, s))));
                help.add(pHelp);
            }
        } else {
            // No JSHelp
            final Documentation.Text text = new Documentation.Text();
            names = text;
            final JSArgument returnAnnotation = method.getAnnotation(JSArgument.class);
            String returnType = returnAnnotation != null ? returnAnnotation.type() : javascriptName(method.getReturnType());

            text.format("%s%s %s(", prefix, returnType, name);

            int startAt = 0;
            if (jsfunction != null && jsfunction.scope())
                startAt = 2;

            Parameter[] parameters = method.getParameters();
            boolean first = true;
            for (int i = startAt; i < parameters.length; i++) {
                String argName = parameters[i].getName();
                String argType = null;

                for (Annotation a : parameters[i].getAnnotations()) {
                    if (a instanceof JSArgument) {
                        final JSArgument jsArg = (JSArgument) a;
                        argName = jsArg.equals("") ? argName : jsArg.name();
                        argType = jsArg.type();
                        if (jsArg.help() != null)
                            pHelp.add(new Documentation.Text(jsArg.name()), new Documentation.Text(jsArg.help()));
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
        }

        methods.add(names, help);
    }

    private static String javascriptName(Class<?> aClass) {
        if (JSBaseObject.class.isAssignableFrom(aClass)) {
            return JSBaseObject.getClassName(aClass);
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
     * Retrieves by introspection the converter and objects defined
     * and prints a documentation out of it
     *
     * @param printer
     */
    static public void printJSHelp(Documentation.Printer printer) {

        // --- Document converter

        printer.append(new Documentation.Title(1, new Documentation.Text("Functions")));

        final Documentation.DefinitionList functions = new Documentation.DefinitionList();

        for (JSUtils.FunctionDefinition d : XPMObject.definitions) {
            final Documentation.Text text = new Documentation.Text(d.getName());
            try {
                final Method method = d.getClazz().getDeclaredMethod("js_" + d.getName(), d.getArguments());
                documentMethod(functions, method, d.getName());
            } catch (NoSuchMethodException e) {
                text.format("Method not found... %s in [%s] with %s", d.getName(), d.getClazz().toString(), Arrays.toString(d.getArguments()));
            }
        }

        printer.append(functions);


        // --- Objects
        printer.append(new Documentation.Title(1, new Documentation.Text("Objects")));
        ArrayList<Class<?>> list = new ArrayList<>();

        Introspection.addImplementors(list, ScriptableObject.class, XPMObject.class.getPackage().getName(), -1);
        Introspection.addImplementors(list, JSBaseObject.class, XPMObject.class.getPackage().getName(), -1);

        final Documentation.DefinitionList classes = new Documentation.DefinitionList();

        for (Class<?> clazz : list) {
            final Documentation.DefinitionList methods = new Documentation.DefinitionList();

            for (Method method : clazz.getDeclaredMethods()) {
                String name = null;

                // A javascript object based on JSBaseObject
                if (JSBaseObject.class.isAssignableFrom(clazz)) {
                    JSFunction annotation = method.getAnnotation(JSFunction.class);
                    if (annotation != null)
                        documentMethod(methods, method, name);
                    continue;
                }

                if (ScriptableObject.class.isAssignableFrom(clazz)) {
                    if (method.getAnnotation(JSFunction.class) != null) {
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

                documentMethod(methods, method, name);
            }
            classes.add(new Documentation.Text(JSBaseObject.getClassName(clazz)), methods);

        }

        printer.append(classes);

    }
}
