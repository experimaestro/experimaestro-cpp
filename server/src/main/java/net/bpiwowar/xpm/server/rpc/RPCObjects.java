package net.bpiwowar.xpm.server.rpc;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.bpiwowar.xpm.commands.AbstractCommand;
import net.bpiwowar.xpm.commands.RootAbstractCommandAdapter;
import net.bpiwowar.xpm.exceptions.XPMCommandException;
import net.bpiwowar.xpm.manager.scripting.Argument;
import net.bpiwowar.xpm.manager.scripting.ConstructorFunction.ConstructorDeclaration;
import net.bpiwowar.xpm.manager.scripting.Declaration;
import net.bpiwowar.xpm.manager.scripting.Expose;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.manager.scripting.Help;
import net.bpiwowar.xpm.manager.scripting.MethodFunction.MethodDeclaration;
import net.bpiwowar.xpm.manager.scripting.Scripting;
import net.bpiwowar.xpm.manager.scripting.WrapperObject;
import net.bpiwowar.xpm.utils.GsonConverter;
import net.bpiwowar.xpm.utils.graphs.Node;
import net.bpiwowar.xpm.utils.graphs.Sort;
import net.bpiwowar.xpm.utils.log.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static com.sun.javafx.binding.StringFormatter.format;

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
        final ArrayList<ConstructorDeclaration> constructors = new ArrayList<>();
        final ArrayList<MethodDeclaration> methods = new ArrayList<>();
        public String className;

        public ClassDescription(Class<?> aClass) {
            this.aClass = aClass;
            if (WrapperObject.class.isAssignableFrom(aClass)) {
                final TypeToken<? extends WrapperObject> wrapperType = TypeToken.of((Class<? extends WrapperObject>) aClass);
                wrappedClass = (Class) wrapperType.resolveType(WrapperObject.class.getTypeParameters()[0]).getType();
            } else {
                wrappedClass = aClass;
            }

            // Get class name
            Exposed exposed = aClass.getAnnotation(Exposed.class);
            className = exposed.value().isEmpty() ? aClass.getSimpleName() : exposed.value();

            // Gather constructors
            for (Constructor<?> constructor : aClass.getDeclaredConstructors()) {
                if (constructor.getAnnotation(Expose.class) != null) {
                    constructors.add(new ConstructorDeclaration(constructor));
                }
            }

            // Gather methods
            for (Method method : aClass.getDeclaredMethods()) {
                // Avoid co-variants
                if (method.isBridge()) continue;
                if (method.getAnnotation(Expose.class) != null) {
                    methods.add(new MethodDeclaration(method));
                }
            }


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

    public static void addRPCMethods(Multimap<String, RPCCaller> methods) throws NoSuchMethodException {
        init();
        methods.put(OBJECTS + ".__delete__", new RPCMethodCaller(
                RPCObjects.class.getMethod("deleteObject", new Class[]{Integer.TYPE}))
        );

        for (ClassDescription classDescription : types.values()) {
            final String prefix = OBJECTS + "." + classDescription.getClassName() + ".";

            for (Declaration<Constructor> constructor : classDescription.constructors) {
                methods.put(prefix + "__init__", new ExposedCaller(constructor));
            }
            for (Declaration<Method> method : classDescription.methods) {
                methods.put(prefix + method.getName(), new ExposedCaller(method));
            }
        }
    }

    public void deleteObject(@RPCArgument(name = "__this__") int id) {
        object2id.remove(objects.get(id));
        objects.set(id, null);
    }

    private long store(Object object) {
        final Integer id = object2id.get(object);
        if (id == null) {
            long newId = objects.size();
            objects.add(object);
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


            if (!isConstructor() && !method.isStatic()) {
                // Add self parameter
                arguments.put("__this__", new MethodArgumentDescriptor("__this__", true, -1));
            }
        }

        boolean isConstructor() {
            return method instanceof ConstructorDeclaration;
        }

        @Override
        public Object call(Object o, JsonObject p) throws InvocationTargetException, IllegalAccessException, InstantiationException {
            RPCObjects objects = (RPCObjects) o;

            Object[] args = new Object[method.getParameterCount()];
            final GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapterFactory(new RootAbstractCommandAdapter());
            Gson gson = builder.create();
            final Type[] types = method.getGenericParameterTypes();
            Object thisObject = null;

            for (MethodArgumentDescriptor descriptor : arguments.values()) {
                final JsonElement jsonElement = p.get(descriptor.name);
                if (descriptor.position == -1) {
                    // This
                    try {
                        int objectId = gson.fromJson(jsonElement, Integer.TYPE);
                        thisObject = objects.objects.get(objectId);
                    } catch (RuntimeException e) {
                        throw new XPMCommandException(e).addContext("while processing parameter %s", descriptor.name);
                    }
                } else {
                    final Type type = types[descriptor.position];
                    try {
                        args[descriptor.position] = gson.fromJson(jsonElement, type);
                    } catch (RuntimeException e) {
                        throw new XPMCommandException(e).addContext("while processing parameter %s", descriptor.name);
                    }
                }
            }

            final Object result = method.invoke(null, thisObject, args);
            if (result == null) return null;

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

        String hppPath = args[0];
        String cppPath = args[1];

        HashMap<Class<?>, ClassNode> nodes = new HashMap<>();
        for (ClassDescription classDescription : RPCObjects.types.values()) {
            nodes.put(classDescription.wrappedClass, new ClassNode(classDescription));
        }

        for (Map.Entry<Class<?>, ClassNode> entry : nodes.entrySet()) {
            Class<?> aClass = entry.getValue().classDescription.aClass;
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

        try (FileOutputStream fos = new FileOutputStream(hppPath);
             PrintStream out = new PrintStream(fos)) {

            out.println("#ifndef _XPM_RPCOBJECTS_H");
            out.println("#define _XPM_RPCOBJECTS_H");
            out.println();
            out.println("#include <vector>");
            out.println("#include <xpm/rpc/utils.hpp>");
            out.println();

            out.format("#ifdef SWIG%n");
            for (ClassDescription classDescription : RPCObjects.types.values()) {
                out.format("%%shared_ptr(xpm::rpc::%s);%n", classDescription.className);
            }
            out.format("#endif%n");

            out.println("namespace xpm { namespace rpc {");


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
                out.format("protected:%n  virtual std::string const &__name__() const override;%n%n");
                out.format("public:%n");

                for (ConstructorDeclaration c : description.constructors) {
                    if (!isRegistered(c)) {
                        LOGGER.error("Unregistered type for %s", c);
                        continue;
                    }

                    outputHelp(out, c);
                    out.print("  ");
                    outputSignature(out, false, description.getClassName(), c);
                    out.println(";");
                }

                for (Declaration<Method> method : description.methods) {
                    if (!isRegistered(method)) {
                        LOGGER.error("Unregistered type for %s", method);
                        continue;
                    }
                    outputHelp(out, method);
                    out.print("  ");
                    outputSignature(out, false, description.getClassName(), method);
                    out.println(";");
                }


                out.format("};%n%n");
            }

            out.println("} }// xpm::rpc namespace");
            out.println("#endif");

        }

        // Outputs c++ definition file
        try (FileOutputStream fos = new FileOutputStream(cppPath);
             PrintStream out = new PrintStream(fos)) {

            out.println("#include <xpm/rpc/objects.hpp>");
            out.println();

            out.format("namespace xpm {%nnamespace rpc{%n");
            for (ClassNode classNode : Lists.reverse(sorted)) {
                final ClassDescription description = classNode.classDescription;
                final String className = description.getClassName();

                String prefix = OBJECTS + "." + className;
                for (Declaration<Constructor> c : description.constructors) {
                    if (!isRegistered(c)) continue;

                    outputSignature(out, true, description.getClassName(), c);
                    generateRPCCall(out, prefix, c);
                }

                out.format("std::string const &%s::__name__() const { static std::string name = \"%s\"; return name; }%n",
                        className, className);


                for (MethodDeclaration method : description.methods) {
                    if (!isRegistered(method)) continue;
                    outputSignature(out, true, className, method);
                    generateRPCCall(out, prefix, method);

                }
            }
            out.println("}} // namespace xpm::rpc");

        }
    }

    public static void outputHelp(PrintStream out, Declaration<?> d) {
        final Help help = d.executable().getAnnotation(Help.class);
        StringBuilder sb = new StringBuilder();

        if (help != null) {
            sb.append(help.value());
            sb.append('\n');
        }

        final Annotation[][] parameterAnnotations = d.getParameterAnnotations();
        final String[] parameterNames = d.getParameterNames();

        for (int i = 0, parameterAnnotationsLength = parameterAnnotations.length; i < parameterAnnotationsLength; i++) {
            Annotation[] parameterAnnotation = parameterAnnotations[i];
            for (Annotation annotation : parameterAnnotation) {
                if (annotation instanceof Argument) {
                    String helpString = ((Argument) annotation).help();
                    if (!helpString.isEmpty()) {
                        sb.append(format("    @param %s %s%n", parameterNames[i], helpString));
                    }
                    break;
                }
            }

        }

        final String s = sb.toString();
        out.print("  /** ");
        out.print(s);
        out.println("  */");

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

    private static void generateRPCCall(PrintStream out, String prefix, Declaration<?> declaration) {
        final Executable executable = declaration.executable();
        String[] parameterNames = declaration.getParameterNames();

        out.println(" {");
        out.println("  nlohmann::json params = nlohmann::json::object();");
        int n = declaration.getParameterCount();
        for (int i = 0; i < n; ++i) {
            out.format("  params[\"%s\"] = RPCConverter<%s>::toJson(%s);%n", parameterNames[i],
                    cppname(executable.getParameterTypes()[i]), parameterNames[i]);
        }

        String callName = declaration.isStatic() ? "__static_call__" : "__call__";

        if (executable instanceof Constructor) {
            out.format("  __set__(%s(\"%s.__init__\", params));%n", callName, prefix);
        } else {
            Type returnType = executable.getAnnotatedReturnType().getType();
            if (returnType == Void.TYPE) {
                out.format("  %s(\"%s.%s\", params);%n", callName, prefix, declaration.getName());
            } else {
                out.format("  return RPCConverter<%s>::toCPP(%s(\"%s.%s\", params));%n", cppname(returnType),
                        callName, prefix, declaration.getName());
            }
        }
        out.format("}%n%n");
    }

    private static void outputSignature(PrintStream out, boolean definition, String className, Declaration<?> declaration) {
        final Executable executable = declaration.executable();
        if (executable instanceof Method) {
            if (declaration.isStatic() && !definition) out.print("static ");
            out.print(cppname(executable.getAnnotatedReturnType().getType()));
            out.print(' ');
        }

        String name = declaration instanceof MethodDeclaration ? declaration.getName() : className;
        if (definition) {
            out.format("%s::%s(", className, name);
        } else {
            out.format("%s(", name);
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
