package net.bpiwowar.xpm.server.rpc;

import net.bpiwowar.xpm.manager.scripting.ClassDescription;
import net.bpiwowar.xpm.manager.scripting.Scripting;

import java.util.stream.Stream;

/**
 * Methods related to documentation*
 */
@JsonRPCMethodsHolder("documentation")
public class DocumentationMethods {
    /** Return registered classes */
    @RPCMethod(help = "Sets a log level")
    public static Stream<String> classes() {
        return Scripting.getTypes().stream().map(t -> ClassDescription.analyzeClass(t).getClassName());
    }
}
