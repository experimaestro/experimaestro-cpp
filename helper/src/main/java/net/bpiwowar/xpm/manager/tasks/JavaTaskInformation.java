package net.bpiwowar.xpm.manager.tasks;

import net.bpiwowar.xpm.manager.Constants;
import net.bpiwowar.xpm.manager.QName;
import net.bpiwowar.xpm.utils.introspection.ClassInfo;
import net.bpiwowar.xpm.utils.introspection.FieldInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

/**
 * All the information about a Java Task
 */
public class JavaTaskInformation {

    /**
     * Task id
     */
    final QName id;

    /**
     * Full class name
     */
    String taskClassname;

    /**
     * The arguments that should be considered as paths
     */
    final ArrayList<PathArgument> pathArguments = new ArrayList<>();

    /**
     * Output type
     */
    QName output;

    Map<String, InputInformation> inputs = new HashMap<>();

    /**
     * Prefixes for namespaces - used for unique directory naming
     */
    Map<String, String> prefixes = new HashMap<>();

    public JavaTaskInformation(ClassInfo classInfo, Map<String, String> namespaces) {
        taskClassname = classInfo.getName();

        namespaces.forEach((key, value) -> prefixes.put(value, key));

        final TaskDescription description = classInfo.getAnnotation(TaskDescription.class);
        if (description == null) {
            throw new RuntimeException(format("The class %s has no TaskDescription annotation", classInfo));
        }

        namespaces.putAll(Constants.PREDEFINED_PREFIXES );
        this.id = QName.parse(description.id(), namespaces);
        this.output = QName.parse(description.output(), namespaces);

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

                final Path path = field.getAnnotation(Path.class);
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
