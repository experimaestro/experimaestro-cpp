package net.bpiwowar.xpm.server.rpc;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.Primitives;
import net.bpiwowar.xpm.exceptions.XPMCommandException;
import net.bpiwowar.xpm.manager.scripting.Argument;
import net.bpiwowar.xpm.manager.scripting.ClassDescription;
import net.bpiwowar.xpm.manager.scripting.ConstructorFunction.ConstructorDeclaration;
import net.bpiwowar.xpm.manager.scripting.Declaration;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.manager.scripting.MethodFunction;
import net.bpiwowar.xpm.manager.scripting.MethodFunction.MethodDeclaration;
import net.bpiwowar.xpm.manager.scripting.Scripting;
import net.bpiwowar.xpm.utils.graphs.Node;
import net.bpiwowar.xpm.utils.graphs.Sort;

import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Expose @Exposed objects to remote calls
 */
public class RPCObjects {

    public static final String OBJECTS = "objects";
    static private boolean initialized = false;
    static private final HashMap<Class<?>, ClassDescription> types = new HashMap<>();

    IdentityHashMap<Object, Integer> object2id = new IdentityHashMap<>();

    ArrayList<Object> objects = new ArrayList<>();


    public RPCObjects() {
    }

    private static void init() {
        if (!initialized) {
            // Initialize flag set
            initialized = true;

            // Retrieve all declared objects
            for (Class<?> aClass : Scripting.getTypes()) {
                types.put(aClass, ClassDescription.analyzeClass(aClass));
            }
        }
    }

    public static void addRPCMethods(Multimap<String, RPCCaller> methods) {
        init();

        for (ClassDescription classDescription : types.values()) {
            final String prefix = OBJECTS + "." + classDescription.getClassName() + ".";

            for (ConstructorDeclaration constructor : classDescription.getConstructors().declarations()) {
                methods.put(prefix + "__init__", new ExposedCaller(constructor));
            }

            // Go through methods
            for (Map.Entry<Object, MethodFunction> entry : classDescription.getMethods().entrySet()) {
                final Object key = entry.getKey();
                if (key instanceof String) {
                    for (MethodDeclaration declaration : entry.getValue().declarations()) {
                        methods.put(prefix, new ExposedCaller(declaration));

                    }
                }
            }
        }
    }

    private long store(Object object) {
        final Integer id = object2id.get(object);
        if (id == null) {
            long newId = objects.size();
            objects.add(id);
            object2id.put(object, id);
            return newId;
        }
        return id;
    }


    final private static class ExposedCaller extends NamedRPCCaller<MethodArgumentDescriptor> {
        /**
         * Method
         */
        Declaration<?> method;


        public ExposedCaller(Declaration<?> callable) {
            this.method = callable;

            Annotation[][] annotations = callable.getParameterAnnotations();

            final String[] parameterNames = method.getParameterNames();
            for (int i = 0; i < annotations.length; i++) {
                arguments.put(parameterNames[i], new MethodArgumentDescriptor(parameterNames[i], true, i));
            }


            if (isConstructor()) {
                // Add self parameter
                arguments.put("__self__", new MethodArgumentDescriptor("__self__", true, -1));
            }
        }

        boolean isConstructor() {
            return method instanceof ConstructorDeclaration;
        }

        @Override
        public Object call(Object o, JsonObject p) throws InvocationTargetException, IllegalAccessException, InstantiationException {
            RPCObjects objects = (RPCObjects) o;

            Object[] args = new Object[method.getParameterCount()];
            Gson gson = new Gson();
            final Type[] types = method.getGenericParameterTypes();

            for (MethodArgumentDescriptor descriptor : arguments.values()) {
                final JsonElement jsonElement = p.get(descriptor.name);
                final Type type = types[descriptor.position];
                try {
                    args[descriptor.position] = gson.fromJson(jsonElement, type);
                } catch (RuntimeException e) {
                    throw new XPMCommandException(e).addContext("while processing parameter %s", descriptor.name);
                }
            }

            final Object result = method.invoke(null, o, args);
            if (result.getClass().getAnnotation(Exposed.class) != null) {
                return objects.store(result);
            }

            return result;
        }

        @Override
        public Class<?> getDeclaringClass() {
            return RPCObjects.class;
        }
    }

    static public class ClassNode implements Node {
        private final ClassDescription classDescription;
        ArrayList<ClassNode> parents = new ArrayList<>();
        ArrayList<ClassNode> children = new ArrayList<>();

        public ClassNode(ClassDescription classDescription) {
            this.classDescription = classDescription;
        }

        @Override
        public Iterable<? extends Node> getParents() {
            return parents;
        }

        @Override
        public Iterable<? extends Node> getChildren() {
            return children;
        }


    }

    static public void main(String[] args) {
        RPCObjects.init();
        PrintStream out = System.out;

        out.println("#ifndef _XPM_RPCOBJECTS_H");
        out.println("#define _XPM_RPCOBJECTS_H");
        out.println();
        out.println("#include <string>");
        out.println("#include <vector>");
        out.println();
        out.println("namespace xpm {");


        out.format("%n%n// Pre-declaration%n");

        // --- Outputs

        HashMap<Class<?>, ClassNode> nodes = new HashMap<>();
        for (ClassDescription classDescription : RPCObjects.types.values()) {
            out.format("class %s;%n", classDescription.getClassName());
            nodes.put(classDescription.getWrappedClass(), new ClassNode(classDescription));
        }

        // --- Outputs classes

        out.println("class ServerObject {");
        out.println("protected:");
        out.println("  int serverId;");
        out.println("  ServerObject();");
        out.println("};");

        out.format("%n%n// Classes%n");

        // Create the class hierarchy
        for (Map.Entry<Class<?>, ClassNode> entry : nodes.entrySet()) {
            Class<?> aClass = entry.getKey();
            for (aClass = aClass.getSuperclass(); !Object.class.equals(aClass); aClass = aClass.getSuperclass()) {
                final ClassNode parentNode = nodes.get(aClass);
                if (parentNode != null) {
                    parentNode.children.add(entry.getValue());
                    entry.getValue().parents.add(parentNode);
                    break;
                }
            }
        }

        final ArrayList<ClassNode> sorted = Sort.topologicalSort(new ArrayList<>(nodes.values()));
        RPCObjects.types.values();

        for (ClassNode classNode : Lists.reverse(sorted)) {
            final ClassDescription description = classNode.classDescription;
            out.format("class %s", classNode.classDescription.getClassName());
            for (int i = 0; i < classNode.parents.size(); ++i) {
                if (i == 0) out.print(" : ");
                else out.print(", ");
                out.print("public ");
                out.print(classNode.parents.get(i).classDescription.getClassName());
            }
            if (classNode.parents.isEmpty())
                out.print(" : ServerObject");
            out.format(" {%n");
            out.format("public:%n");

            for (MethodFunction methods : description.getMethods().values()) {
                if (methods == null) {
                    continue;
                }

                final Iterable<MethodDeclaration> declarations = methods.declarations();
                if (declarations == null) {
                    continue;
                }

                for (MethodDeclaration method : declarations) {
                    final Executable executable = method.executable();

                    out.format("  %s %s(", cppname(executable.getAnnotatedReturnType().getType()), methods.getKey());
                    final String[] parameterNames = method.getParameterNames();

                    for (int i = 0; i < method.executable().getParameterCount(); ++i) {
                        if (i > 0) out.print(", ");
                        out.format("%s const &%s", cppname(executable.getParameterTypes()[i]), parameterNames[i]);
                    }
                    out.println(");");
                }

            }


            out.format("};%n%n");
        }

        out.println("} // xpm namespace");
        out.println("#endif");

    }

    static final HashMap<Type, String> type2cppType = new HashMap<>();

    static {
        type2cppType.put(String.class, "std::string");
        type2cppType.put(Void.TYPE, "void");

        type2cppType.put(Boolean.TYPE, "bool");
        type2cppType.put(Long.TYPE, "int64_t");
        type2cppType.put(Integer.TYPE, "int32_t");
        type2cppType.put(Float.TYPE, "float");
        type2cppType.put(Double.TYPE, "double");
    }

    private static String cppname(Type type) {
        final String s = type2cppType.get(type);
        if (s != null) {
            return s;
        }

        final ClassDescription description = types.get(type);
        if (description != null) {
            return "std::shared_ptr<" + description.getClassName() + ">";
        }

        if (type instanceof ParameterizedType) {
            return "UNKNOWN_PTYPE(" + type.toString() + ")";
        }

        Class<?> aClass = (Class)type;
        if (aClass.isArray()) {
            return "std::array<" + cppname(aClass.getComponentType()) + ">";
        }

        return "UNKNOWN(" + type.toString() + ")";
//        throw new AssertionError("Cannot handle type: " + type);
    }
}
