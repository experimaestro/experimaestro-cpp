package net.bpiwowar.xpm.server.rpc;

import java.lang.annotation.Annotation;

/**
 * Annotation instance class
 */
public class RPCArrayArgument implements RPCArgument {
    @Override
    public String name() {
        return null;
    }

    @Override
    public boolean required() {
        return true;
    }

    @Override
    public String help() {
        return "Array element";
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return RPCArgument.class;
    }
}
