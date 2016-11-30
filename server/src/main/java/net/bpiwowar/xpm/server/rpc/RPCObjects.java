package net.bpiwowar.xpm.server.rpc;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.bpiwowar.xpm.exceptions.XPMCommandException;
import net.bpiwowar.xpm.manager.scripting.*;
import net.bpiwowar.xpm.manager.scripting.ConstructorFunction.ConstructorDeclaration;
import net.bpiwowar.xpm.manager.scripting.MethodFunction.MethodDeclaration;
import net.bpiwowar.xpm.utils.graphs.Node;
import net.bpiwowar.xpm.utils.graphs.Sort;
import net.bpiwowar.xpm.utils.log.Logger;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

/**
 * Expose @Exposed objects to remote calls
 */
public class RPCObjects {

    public static final String OBJECTS = "objects";
    private static final Logger LOGGER = Logger.getLogger();
    static private boolean initialized = false;
    static private final HashMap<Class<?>, ClassDescription> types = new HashMap<>();

    IdentityHashMap<Object, Integer> object2id = new IdentityHashMap<>();

    ArrayList<Object> objects = new ArrayList<>();


    public RPCObjects() {
    }

    static public class ClassDescription {
        final Class<?> wrappedClass;
        final Class<?> aClass;
        final ArrayList<Constructor> constructors = new ArrayList<>();
        final ArrayList<Method> methods = new ArrayList<>();
        public String className;

        public ClassDescription(Class<?> aClass) {
            this.aClass = aClass;
            if (WrapperObject.class.isAssignableFrom(aClass)) {
                final TypeToken<? extends WrapperObject> wrapperType = TypeToken.of((Class<? extends WrapperObject>) aClass);
                wrappedClass = (Class) wrapperType.resolveType(WrapperObject.class.getTypeParameters()[0]).getType();
            } else {
                wrappedClass = aClass;
            }

            // Gather constructors
            for (Constructor<?> constructor : aClass.getDeclaredConstructors()) {
                if (constructor.getAnnotation(Exposed.class) != null) {
                    constructors.add(constructor);
                }
            }

            // Gather methods
            for (Method method : aClass.getDeclaredMethods()) {
                if (method.getAnnotation(Exposed.class) != null) {
                    methods.add(method);
                }
            }

            Exposed exposed = aClass.getAnnotation(Exposed.class);
            className = exposed.value().isEmpty() ? aClass.getSimpleName() : exposed.value();

        }

        public String getClassName() {
            return className;
        }
    }

    private static void init() {
        if (!initialized) {
            // Initialize flag set
            initialized = true;

            // Retrieve all declared objects
            for (Class<?> aClass : Scripting.getTypes()) {
                ClassDescription cd = new ClassDescription(aClass);
                types.put(cd.wrappedClass, cd);
            }
        }
    }

    public static void addRPCMethods(Multimap<String, RPCCaller> methods) {
        init();

        for (ClassDescription classDescription : types.values()) {
            final String prefix = OBJECTS + "." + classDescription.getClassName() + ".";

            for (Constructor constructor : classDescription.constructors) {
                methods.put(prefix + "__init__", new ExposedCaller(constructor));
            }
            for (Method method : classDescription.methods) {
                methods.put(prefix, new ExposedCaller(method));
            }
        }
    }

    private static String getMethodName(Method method) {
        Expose expose = method.getAnnotation(Expose.class);
        String name = null;
        if (expose != null) name = expose.value();
        if (name != null && !name.isEmpty())
            return name;
        return method.getName();
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
        Executable method;


        public ExposedCaller(Executable callable) {
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

    static public void main(String[] args) throws IOException {
        RPCObjects.init();

        String basepath = args[0];

        HashMap<Class<?>, ClassNode> nodes = new HashMap<>();
        for (ClassDescription classDescription : RPCObjects.types.values()) {
            nodes.put(classDescription.wrappedClass, new ClassNode(classDescription));
        }

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

        try (FileOutputStream fos = new FileOutputStream(basepath + ".hpp");
             PrintStream out = new PrintStream(fos)) {

            out.println("#ifndef _XPM_RPCOBJECTS_H");
            out.println("#define _XPM_RPCOBJECTS_H");
            out.println();
            out.println("#include <vector>");
            out.println("#include <xpm/rpc/utils.hpp>");
            out.println();
            out.println("namespace xpm {");


            out.format("%n%n// Pre-declaration%n");

            // --- Outputs

            for (ClassDescription classDescription : RPCObjects.types.values()) {
                out.format("class %s;%n", classDescription.className);
            }

            // --- Outputs classes


            out.format("%n%n// Classes%n");


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
                    out.print(" : public ServerObject");
                out.format(" {%n");
                out.format("public:%n");

                for (ConstructorDeclaration c : description.getConstructors().declarations()) {
                    if (!isRegistered(c)) {
                        LOGGER.error("Unregistered type for %s", c);
                        continue;
                    }

                    out.print("  ");
                    outputSignature(out, null, description.getClassName(), c);
                    out.println(";");
                }

                for (MethodFunction methods : description.getMethods().values()) {
                    if (methods == null) {
                        continue;
                    }

                    final Iterable<MethodDeclaration> declarations = methods.declarations();
                    if (declarations == null) {
                        continue;
                    }


                    for (MethodDeclaration method : declarations) {
                        if (!isRegistered(method)) {
                            LOGGER.error("Unregistered type for %s", method);
                            continue;
                        }
                        out.print("  ");
                        outputSignature(out, null, methods.getKey(), method);
                        out.println(";");
                    }

                }


                out.format("};%n%n");
            }

            out.println("} // xpm namespace");
            out.println("#endif");

        }

        // Outputs c++ definition file
        try (FileOutputStream fos = new FileOutputStream(basepath + ".cpp");
             PrintStream out = new PrintStream(fos)) {

            out.println("#include <xpm/rpc/objects.hpp>");
            out.println();

            out.format("namespace xpm {%n");
            for (ClassNode classNode : Lists.reverse(sorted)) {
                final ClassDescription description = classNode.classDescription;

                for (ConstructorDeclaration c : description.getConstructors().declarations()) {
                    if (!isRegistered(c)) continue;

                    outputSignature(out, description, description.getClassName(), c);
                    generateRPCCall(out, c);
                }

                for (MethodFunction methods : description.getMethods().values()) {
                    if (methods == null) {
                        continue;
                    }

                    final Iterable<MethodDeclaration> declarations = methods.declarations();
                    if (declarations == null) {
                        continue;
                    }

                    for (MethodDeclaration method : declarations) {
                        if (!isRegistered(method)) continue;
                        outputSignature(out, description, methods.getKey(), method);
                        generateRPCCall(out, method);
                    }
                }
            }
            out.println("}");

        }
    }

    private static boolean isRegistered(Declaration<?> declaration) {
        if (!isRegistered(declaration.getReturnType())) {
            LOGGER.warn("Unregistered %s", declaration.getReturnType());
            return false;
        }
        Type[] types = declaration.getGenericParameterTypes();
        for (int i = declaration.getParameterCount(); --i >= 0; ) {
            if (!isRegistered(types[i])) {
                LOGGER.warn("Unregistered %s", types[i]);
                return false;
            }
        }
        return true;
    }

    private static void generateRPCCall(PrintStream out, Declaration<?> declaration) {
        String[] parameterNames = declaration.getParameterNames();

        out.println(" {");
        out.println("  json params = json::object();");
        int n = declaration.getParameterCount();
        for (int i = 0; i < n; ++i) {
            out.format("  params[\"%s\"] = cpp2rpc(%s);%n", parameterNames[i], parameterNames[i]);
        }

        String _static = declaration.isStatic() ? "__static_call__" : "__call__";

        if (declaration.executable() instanceof Constructor) {
            out.format("  __set__(%s(params));%n", _static);
        } else {
            Type returnType = declaration.executable().getAnnotatedReturnType().getType();
            if (returnType == Void.TYPE) {
                out.format("  %s(params);%n", _static);
            } else {
                out.format("  return rpc2cpp<%s>(%s(params));%n", cppname(returnType), _static);
            }
        }
        out.format("}%n%n");
    }

    private static void outputSignature(PrintStream out, ClassDescription description,
                                        String name, Declaration<?> declaration) {
        final Executable executable = declaration.executable();
        if (executable instanceof Method) {
            if (declaration.isStatic()) out.print("static ");
            out.print(cppname(executable.getAnnotatedReturnType().getType()));
            out.print(' ');
        }

        if (description == null) {
            out.format("%s(", name);
        } else {
            out.format("%s::%s(", description.getClassName(), name);
        }

        final String[] parameterNames = declaration.getParameterNames();

        for (int i = 0; i < declaration.executable().getParameterCount(); ++i) {
            if (i > 0) out.print(", ");
            out.format("%s const &%s", cppname(executable.getParameterTypes()[i]), parameterNames[i]);
        }
        out.print(")");
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

    private static boolean isRegistered(Type type) {
        return cppname(type) != null;
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
            return null;
        }
        if (type instanceof TypeVariable) {
            return null;
        }

        Class<?> aClass = (Class) type;
        if (aClass.isArray()) {
            String cppname = cppname(aClass.getComponentType());
            return cppname == null ? null : "std::vector<" + cppname + ">";
        }

        if (List.class.isAssignableFrom(aClass)) {
            final TypeToken<? extends List> wrapperType = TypeToken.of((Class<? extends List>) aClass);

            String cppname = cppname(wrapperType.resolveType(List.class.getTypeParameters()[0]).getType());

            return cppname == null ? null : "std::vector<" + cppname + ">";

        }

        return null;
    }
}
