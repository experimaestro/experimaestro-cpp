package sf.net.experimaestro.manager.js;

import bpiwowar.argparser.utils.Introspection;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.annotations.JSFunction;
import sf.net.experimaestro.utils.Documentation;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.Output;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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

        if (name == null) {
            final JSFunction annotation = method.getAnnotation(JSFunction.class);
            if (annotation == null) {
                return;
            }
            name = annotation.value();
        }

        if (name == null) {
            name = method.getName();
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
                final String s = Output.toString(", ", arguments.value(), new Output.Formatter<JSArgument>() {
                    @Override
                    public String format(JSArgument o) {
                        if (o.help() != null) {
                            pHelp.add(new Documentation.Text(o.name()), new Documentation.Text(o.help()));
                        }
                        return String.format("%s %s", o.type(), o.name());
                    }
                });
                String returnType = arguments.returnType();
                if ("".equals(returnType))
                    returnType = method.getReturnType().toString();
                container.add(new Documentation.Division(
                        new Documentation.Text(format("%s %s(%s)\n", returnType, name, s))));
                help.add(pHelp);
            }
        } else {
            final Documentation.Text text = new Documentation.Text();
            names = text;
            final JSArgument returnAnnotation = method.getAnnotation(JSArgument.class);
            String returnType = returnAnnotation != null ? returnAnnotation.type() : method.getReturnType().toString();

            text.format("%s %s(", returnType, name);

            final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            final Class<?>[] parameterTypes = method.getParameterTypes();
            boolean first = true;
            for (int i = 0; i < parameterTypes.length; i++) {
                String argName = "";
                String argType = null;

                for (Annotation a : parameterAnnotations[i]) {
                    if (a instanceof JSArgument) {
                        final JSArgument jsArg = (JSArgument) a;
                        argName = jsArg.name();
                        argType = jsArg.type();
                        if (jsArg.help() != null)
                            pHelp.add(new Documentation.Text(jsArg.name()), new Documentation.Text(jsArg.help()));
                    }
                }
                if (!first)
                    text.append(", ");
                else
                    first = false;

                if (argType == null) {
                    Class<?> pClass = (Class<?>) parameterTypes[i];
                    argType = pClass.toString();
                }

                text.format("%s %s", argType, argName);
            }
            text.append(")");
        }

        methods.add(names, help);
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
                if (ScriptableObject.class.isAssignableFrom(clazz)) {
                    if (clazz.getAnnotation(JSFunction.class) != null) {
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
