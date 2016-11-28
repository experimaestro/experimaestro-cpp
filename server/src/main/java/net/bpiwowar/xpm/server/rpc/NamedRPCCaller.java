package net.bpiwowar.xpm.server.rpc;

import com.google.gson.JsonObject;

import java.util.HashMap;

/**
 *
 */
public abstract class NamedRPCCaller<T extends ArgumentDescriptor> extends RPCCaller<T> {
    /**
     * Arguments
     */
    HashMap<String, T> arguments = new HashMap<>();

    @Override
    public int score(JsonObject p) {
        int score = Integer.MAX_VALUE;
        for (Object _argument : arguments.values()) {
            ArgumentDescriptor argument = (ArgumentDescriptor) _argument;
            final boolean has = p.has(argument.name);
            if (argument.required && !has) {
                return Integer.MIN_VALUE;
            }
            if (!has) --score;
        }
        return score;
    }
}
