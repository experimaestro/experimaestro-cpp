package net.bpiwowar.xpm.server.rpc;

import com.google.gson.JsonObject;

/**
 *
 */
public abstract class RPCCaller<T extends ArgumentDescriptor> {
    public abstract Object call(Object o, JsonObject p) throws Throwable;

    public abstract Class<?> getDeclaringClass();


    public abstract int score(JsonObject p);
}
