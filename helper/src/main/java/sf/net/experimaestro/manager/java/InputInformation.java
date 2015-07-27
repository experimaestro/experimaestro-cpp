package sf.net.experimaestro.manager.java;

import net.bpiwowar.experimaestro.tasks.JsonArgument;
import sf.net.experimaestro.manager.Constants;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.utils.introspection.ClassInfo;
import sf.net.experimaestro.utils.introspection.FieldInfo;

/**
 *
 */
public class InputInformation {
    private final QName valueType;
    final String help;
    final boolean required;

    public InputInformation(FieldInfo field) {
        this.valueType = getType(field.getType());
        JsonArgument jsonArgument = field.getAnnotation(JsonArgument.class);
        help = jsonArgument.help();
        required = jsonArgument.required();

    }

    private QName getType(ClassInfo type) {
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

        // Otherwise, just return any
        return Constants.XP_ANY;
    }

    public QName getValueType() {
        return valueType;
    }

}
