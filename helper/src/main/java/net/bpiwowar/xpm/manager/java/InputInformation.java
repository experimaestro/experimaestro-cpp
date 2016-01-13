package sf.net.experimaestro.manager.java;

import net.bpiwowar.experimaestro.tasks.JsonArgument;
import sf.net.experimaestro.manager.Constants;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.tasks.Type;
import sf.net.experimaestro.utils.introspection.AnnotationInfo;
import sf.net.experimaestro.utils.introspection.ClassInfo;
import sf.net.experimaestro.utils.introspection.FieldInfo;

import java.util.Map;

import static sf.net.experimaestro.manager.QName.parse;

/**
 *
 */
public class InputInformation {
    private final QName valueType;
    final String help;
    final boolean required;

    public InputInformation(Map<String, String> namespaces, FieldInfo field) {
        JsonArgument jsonArgument = field.getAnnotation(JsonArgument.class);
        this.valueType = getType(jsonArgument, field.getType(), namespaces);
        help = jsonArgument.help();
        required = jsonArgument.required();
    }

    private QName getType(JsonArgument jsonArgument, ClassInfo type, Map<String, String> namespaces) {
        if (type.belongs(java.lang.Integer.class) || type.belongs(Integer.TYPE)
                || type.belongs(java.lang.Long.class) || type.belongs(Long.TYPE)
                || type.belongs(Short.class) || type.belongs(Short.TYPE)) {
            return Constants.XP_INTEGER;
        }

        if (type.belongs(java.lang.Double.class) || type.belongs(java.lang.Float.class)) {
            return Constants.XP_REAL;
        }

        if (type.belongs(String.class))
            return Constants.XP_STRING;

        // Get from class
        final Type typeInfo = type.getAnnotation(Type.class);
        if (typeInfo != null) {
            return parse(typeInfo.type(), namespaces);
        }

        // Check the type
        if (!jsonArgument.type().isEmpty()) {
            return parse(jsonArgument.type(), namespaces);
        }

        // Otherwise, just return any
        return Constants.XP_ANY;
    }

    public QName getValueType() {
        return valueType;
    }

}
