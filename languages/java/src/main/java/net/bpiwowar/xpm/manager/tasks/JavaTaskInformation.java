package net.bpiwowar.xpm.manager.tasks;

import net.bpiwowar.xpm.manager.Constants;
import net.bpiwowar.xpm.manager.Typename;
import net.bpiwowar.xpm.utils.introspection.ClassInfo;
import net.bpiwowar.xpm.utils.introspection.FieldInfo;

import java.util.Map;

import static java.lang.String.format;

/**
 * All the information about a Java Task
 */
public class JavaTaskInformation extends TaskInformation {

    /**
     * Full class name
     */
    String taskClassname;

    public JavaTaskInformation(ClassInfo classInfo, Map<String, String> namespaces) {
        taskClassname = classInfo.getName();

        namespaces.forEach((key, value) -> prefixes.put(value, key));

        final TaskDescription description = classInfo.getAnnotation(TaskDescription.class);
        if (description == null) {
            throw new RuntimeException(format("The class %s has no TaskDescription annotation", classInfo));
        }

        namespaces.putAll(Constants.PREDEFINED_PREFIXES );
        this.id = Typename.parse(description.id(), namespaces);
        this.output = Typename.parse(description.output(), namespaces);

        try {
            for (FieldInfo field : classInfo.getDeclaredFields()) {
                //final Object jsonArgument = field.getAnnotation(jsonArgumentClass.class);
                final JsonArgument jsonArgument = field.getAnnotation(JsonArgument.class);

                // TODO: add default values, etc.
                String fieldName = field.getName();
                if (jsonArgument != null) {
                    String name = getString(jsonArgument.name(), fieldName);
                    inputs.put(name, new InputInformation(namespaces, field));
                }

                final JsonPath path = field.getAnnotation(JsonPath.class);
                if (path != null) {
                    String copy = getString(path.copy(), fieldName);
                    String relativePath = getString(path.value(), fieldName);
                    pathArguments.add(new PathArgument(copy, relativePath));
                }
            }
        } catch(RuntimeException e) {
            System.err.format("Error while analyzing class %s%n", taskClassname);
            throw e;
        }

    }




    private static String getString(String value, String defaultValue) {
        return "".equals(value) ? defaultValue : value;
    }

}
