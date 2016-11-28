package net.bpiwowar.xpm.server.rpc;

import java.lang.reflect.Field;

/**
 *
 */
public class ClassArgumentDescriptor extends ArgumentDescriptor {

    final Field field;

    public ClassArgumentDescriptor(RPCArgument annotation, Field field) {
        super(annotation);
        final String name = annotation.name();
        this.name = name.isEmpty() ? field.getName() : name;
        this.field = field;
    }
}
