package net.bpiwowar.xpm.server.rpc;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Interface for
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonRPCMethodsHolder {
    /** The name */
    String value();
}
