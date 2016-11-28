package net.bpiwowar.xpm.server.rpc;

/**
 *
 */
public class MethodArgumentDescriptor extends ArgumentDescriptor {
    int position;

    public MethodArgumentDescriptor(RPCArgument annotation, int position) {
        super(annotation);
        this.position = position;
    }

    public MethodArgumentDescriptor(String name, boolean required, int position) {
        super(name, required);
        this.position = position;
    }
}
