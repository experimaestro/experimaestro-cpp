package net.bpiwowar.xpm.server.rpc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.bpiwowar.xpm.manager.scripting.ClassDescription;
import net.bpiwowar.xpm.manager.scripting.Scripting;

import java.util.stream.Stream;

/**
 * Methods related to documentation*
 */
@JsonRPCMethodsHolder("documentation")
public class DocumentationMethods {
    /**
     * Return registered classes
     */
    @RPCMethod(help = "Get the classes pre-defined for scripts")
    public static Stream<String> classes() {
        return Scripting.getTypes().stream().map(t -> ClassDescription.analyzeClass(t).getClassName());
    }

    @RPCMethod(help = "Get the classes pre-defined for scripts")
    public static JsonObject methods(
            @RPCArgument(name = "classname") String classname
    ) {
        JsonObject response = new JsonObject();
        ClassDescription cd = Scripting.getClassDescription(classname);

        JsonArray constructors = new JsonArray();
        response.add("constructors", constructors);
        cd.getConstructors().declarations().forEach(declaration -> {
            constructors.add(new JsonPrimitive(declaration.toString()));
        });

        JsonObject methods = new JsonObject();
        response.add("constructors", methods);
        cd.getMethods().forEach((key, method) -> {
        });
        return response;
    }

}
