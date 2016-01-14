package net.bpiwowar.xpm.manager.tasks;

import net.bpiwowar.xpm.manager.Constants;
import net.bpiwowar.xpm.manager.QName;
import net.bpiwowar.xpm.utils.introspection.ClassInfo;
import net.bpiwowar.xpm.utils.introspection.FieldInfo;

import java.util.Map;

import static net.bpiwowar.xpm.manager.QName.parse;

/**
 *
 */
public class InputInformation {
    private final QName valueType;
    final String help;
    final String copyTo;
    final boolean required;

    public InputInformation(Map<String, String> namespaces, FieldInfo field) {
        JsonArgument jsonArgument = field.getAnnotation(JsonArgument.class);
        this.valueType = getType(jsonArgument, field.getType(), namespaces);
        help = jsonArgument.help();
        required = jsonArgument.required();
        copyTo = jsonArgument.copyTo();
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
        final JsonType jsonTypeInfo = type.getAnnotation(JsonType.class);
        if (jsonTypeInfo != null) {
            return parse(jsonTypeInfo.type(), namespaces);
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
