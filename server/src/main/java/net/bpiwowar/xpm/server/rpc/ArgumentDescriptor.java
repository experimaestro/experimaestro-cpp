package net.bpiwowar.xpm.server.rpc;

/**
 *
 */
public class ArgumentDescriptor {
    String name;
    boolean required;

    ArgumentDescriptor(RPCArgument annotation) {
        this.required = annotation.required();
        this.name = annotation.name();
    }

    public ArgumentDescriptor(String name, boolean required) {
        this.name = name;
        this.required = required;
    }
}
