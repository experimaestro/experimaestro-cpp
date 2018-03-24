package net.bpiwowar.xpm.server.rpc;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import net.bpiwowar.xpm.exceptions.XPMCommandException;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.manager.scripting.*;
import net.bpiwowar.xpm.manager.scripting.ConstructorFunction.ConstructorDeclaration;
import net.bpiwowar.xpm.manager.scripting.MethodFunction.MethodDeclaration;
import net.bpiwowar.xpm.utils.graphs.Node;
import net.bpiwowar.xpm.utils.graphs.Sort;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

import static java.lang.String.format;

/**
 * Expose @Exposed objects to remote calls
 */
public class RPCObjects implements AutoCloseable {

    private static final String OBJECTS = "objects";
    private static final Logger LOGGER = LogManager.getFormatterLogger();
    static private boolean initialized = false;
    static private final HashMap<Class<?>, ClassDescription> types = new HashMap<>();
    private final Context context;

    IdentityHashMap<Object, Integer> object2id = new IdentityHashMap<>();

    Int2ObjectLinkedOpenHashMap<Object> objects = new Int2ObjectLinkedOpenHashMap<>();
    int currentId = 0;

    public RPCObjects(JsonRPCMethods mos, JsonRPCSettings settings) {
        context = new Context(settings.scheduler);
    }

    @Override
    public void close() throws Exception {
        context.close();
    }


    static public class ClassDescription {
        final Class<?> wrappedClass;
        final Class<?> wrapperClass;
        final ArrayList<ConstructorDeclaration> constructors = new ArrayList<>();
        final ArrayList<MethodDeclaration> methods = new ArrayList<>();
        public String className;
        private final Constructor<?> constructor;

        public Object wrap(Object object) {
            if (!wrappedClass.isInstance(object))
                 throw new IllegalArgumentException(format("Cannot wrap %s into %s", object.getClass(), wrappedClass.getClass()));
            if (constructor != null) {
                try {
                    return constructor.newInstance(object);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new AssertionError("Unexpected construction error", e);
                }
            }
            return object;
        }

        public ClassDescription(Class<?> aClass) throws NoSuchMethodException {
            this.wrapperClass = aClass;
            if (WrapperObject.class.isAssignableFrom(aClass)) {
                final TypeToken<? extends WrapperObject> wrapperType = TypeToken.of((Class<? extends WrapperObject>) aClass);
                wrappedClass = (Class) wrapperType.resolveType(WrapperObject.class.getTypeParameters()[0]).getType();
                constructor = wrapperClass.getConstructor(wrappedClass);
            } else {
                wrappedClass = aClass;
                constructor = null;
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
                final Expose annotation = method.getAnnotation(Expose.class);
                if (annotation != null) {
                    methods.add(new MethodDeclaration(method));
                }
            }


        }

        public String getClassName() {
            return className;
        }
    }

    private static void init() throws NoSuchMethodException {
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

    synchronized public void deleteObject(@RPCArgument(name = "__this__") int id) {
        Object object = objects.get(id);
        object2id.remove(object);
        objects.remove(id);
    }

    private long store(Object object) {
        object = RPCObjects.this.wrap(object);
        // Store
        final Integer id = object2id.get(object);
        if (id == null) {
            int newId = currentId++;
            objects.put(newId, object);
            object2id.put(object, newId);
            return newId;
        }
        return id;
    }

    private Object wrap(Object object) {
        // Try to wrap
        final ClassDescription cd = findDefinition(object.getClass());
        if (cd == null) {
            throw new IllegalArgumentException(format("Could not find wrapper for class %s", object.getClass()));
        }
        return cd.wrap(object);
    }

    private ClassDescription findDefinition(Class<?> aClass) {
        if (aClass == null || aClass == Object.class) return null;

        ClassDescription cd = types.get(aClass);
        if (cd != null) return cd;

        for (Class<?> anInterface : aClass.getInterfaces()) {
            cd = findDefinition(anInterface);
            if (cd != null) return cd;
        }

        cd = findDefinition(aClass.getSuperclass());
        if (cd != null) return cd;

        return null;
    }


    final private static class ExposedCaller extends NamedRPCCaller<MethodArgumentDescriptor> {
        /**
         * Method
         */
        Declaration<?> method;


        ExposedCaller(Declaration<?> callable) {
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
            RPCObjects rpcObjects = (RPCObjects) o;

            Object[] args = new Object[method.getParameterCount()];
            final GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapterFactory(rpcObjects.adapterFactory);
            Gson gson = builder.create();
            final Type[] types = method.getGenericParameterTypes();
            Object thisObject = null;

            for (MethodArgumentDescriptor descriptor : arguments.values()) {
                final JsonElement jsonElement = p.get(descriptor.name);
                try {
                    if (descriptor.position == -1) {
                        // This
                        int objectId = gson.fromJson(jsonElement, Integer.TYPE);
                        thisObject = rpcObjects.objects.get(objectId);
                    } else {
                        args[descriptor.position] = unwrap(gson.fromJson(jsonElement, types[descriptor.position]));
                    }
                } catch (XPMRuntimeException e) {
                    throw e.addContext("while processing parameter %s", descriptor.name);
                } catch (RuntimeException e) {
                    throw new XPMCommandException(e).addContext("while processing parameter %s", descriptor.name);
                }
            }

            rpcObjects.context.setThreadScriptContext();
            final Object result = method.invoke(thisObject, args);

            if (result != null && rpcObjects.isManaged(result.getClass())) {
                return rpcObjects.store(result);
            }

            return result;
        }

        private Object unwrap(Object o) {
            if (o instanceof Wrapper) return ((Wrapper) o).unwrap();
            return o;
        }


        @Override
        public Class<?> getDeclaringClass() {
            return RPCObjects.class;
        }

    }

    private static HashSet<Class<?>> UNMANAGED_TYPES = new HashSet<>();

    static {
        UNMANAGED_TYPES.add(boolean.class);
        UNMANAGED_TYPES.add(Boolean.class);
        UNMANAGED_TYPES.add(byte.class);
        UNMANAGED_TYPES.add(Byte.class);
        UNMANAGED_TYPES.add(char.class);
        UNMANAGED_TYPES.add(Character.class);
        UNMANAGED_TYPES.add(double.class);
        UNMANAGED_TYPES.add(Double.class);
        UNMANAGED_TYPES.add(float.class);
        UNMANAGED_TYPES.add(Float.class);
        UNMANAGED_TYPES.add(int.class);
        UNMANAGED_TYPES.add(Integer.class);
        UNMANAGED_TYPES.add(long.class);
        UNMANAGED_TYPES.add(Long.class);
        UNMANAGED_TYPES.add(short.class);
        UNMANAGED_TYPES.add(Short.class);
        UNMANAGED_TYPES.add(void.class);
        UNMANAGED_TYPES.add(Void.class);
        UNMANAGED_TYPES.add(String.class);
    }

    /**
     * Checks whether the type is managed by RPC objects
     *
     * @param aClass Class
     * @return True if the class is managed
     */
    private boolean isManaged(Class<?> aClass) {
        return !UNMANAGED_TYPES.contains(aClass)
                && !aClass.isArray()
                && !(List.class.isAssignableFrom(aClass));
    }

    class RPCAdapterFactory implements AnnotatedTypeAdapterFactory {
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeAttributes attributes, com.google.gson.reflect.TypeToken<T> type) {
            if (!isManaged(type.getRawType())) {
                return null;
            }
            return new TypeAdapter<T>() {
                @Override
                public void write(JsonWriter out, T value) throws IOException {
                    throw new RuntimeException("Not writable object");
                }

                @Override
                public T read(JsonReader in) throws IOException {
                    int objectId = in.nextInt();
                    if (objectId < 0) return null;

                    Object storedObject = RPCObjects.this.objects.get(objectId);
                    if (storedObject == null) {
                        throw new XPMCommandException("Object ID not registered (%d)", objectId);
                    }
                    return (T) storedObject;
                }
            };
        }
    }

    RPCAdapterFactory adapterFactory = new RPCAdapterFactory();

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

    static public void main(String[] args) throws IOException, NoSuchMethodException {
        RPCObjects.init();

        String hppPath = args[0];
        String cppPath = args[1];

        HashMap<Class<?>, ClassNode> nodes = new HashMap<>();
        for (ClassDescription classDescription : RPCObjects.types.values()) {
            nodes.put(classDescription.wrappedClass, new ClassNode(classDescription));
        }

        // Build the hierarchy
        for (Map.Entry<Class<?>, ClassNode> entry : nodes.entrySet()) {
            ClassNode node = entry.getValue();
            Class<?> aClass = node.classDescription.wrapperClass;
            addInterfaces(nodes, node, aClass);

            for (aClass = aClass.getSuperclass(); aClass != null && !Object.class.equals(aClass); aClass = aClass.getSuperclass()) {
                if (addParent(nodes, node, aClass)) break;
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
            out.println("#include <xpm/rpc/optional.hpp>");
            out.println();

            out.format("#ifdef SWIG%n");
            for (ClassDescription classDescription : RPCObjects.types.values()) {
                out.format("%%shared_ptr(xpm::rpc::%s);%n", classDescription.className);
            }
            out.format("#endif%n");
            out.format("#if defined(SWIGJAVA) && defined(SWIG) %n");
            for (ClassDescription classDescription : RPCObjects.types.values()) {
                out.format("%%nspace xpm::rpc::%s;%n", classDescription.className);
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
                boolean isInterface = description.wrapperClass.isInterface();

                out.format("class %s", classNode.classDescription.getClassName());
                for (int i = 0; i < classNode.parents.size(); ++i) {
                    if (i == 0) out.print(" : ");
                    else out.print(", ");
                    out.print("public ");
                    out.print(classNode.parents.get(i).classDescription.getClassName());
                }
                if (classNode.parents.isEmpty()) {
                    out.print(" : public virtual ServerObject");
                }
                out.format(" {%n");
                out.println("protected:");
                out.format("  friend struct RPCConverter<std::shared_ptr<%s>>;%n", description.getClassName());
                out.format("  explicit %s(ObjectIdentifier o);%n", description.getClassName());
                if (!isInterface)
                    out.format("  virtual std::string const &__name__() const override;%n%n");
                if (description.constructors.isEmpty()) {
                    out.format("  %s() {}%n%n", description.getClassName());
                }
                out.format("public:%n");

                for (ConstructorDeclaration c : description.constructors) {
                    outputDeclaration(out, description, c);
                }

                for (Declaration<Method> method : description.methods) {
                    outputDeclaration(out, description, method);
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
                boolean isInterface = description.wrapperClass.isInterface();
                final String className = description.getClassName();

                String prefix = OBJECTS + "." + className;
                for (Declaration<Constructor> c : description.constructors) {
                    if (!isRegistered(c)) continue;

                    outputSignature(out, true, description.getClassName(), c);
                    generateRPCCall(out, prefix, c);
                }
                if (!isInterface) {
                    out.format("std::string const &%s::__name__() const { static std::string name = \"%s\"; return name; }%n",
                            className, className);
                }

                final ClassNode parentNode = classNode.parents.isEmpty() ? null : classNode.parents.get(0);
                out.format("%s::%s(ObjectIdentifier o) : ServerObject(o)%s {} %n",
                        description.getClassName(), description.getClassName(),
                        parentNode == null ? "" : ", " + parentNode.classDescription.getClassName() + "(o)");

                for (MethodDeclaration method : description.methods) {
                    if (!isRegistered(method) || method.isPureVirtual()) continue;
                    outputSignature(out, true, className, method);
                    generateRPCCall(out, prefix, method);

                }
            }
            out.println("}} // namespace xpm::rpc");

        }
    }

    private static void outputDeclaration(PrintStream out, ClassDescription description, Declaration<?> c) {
        if (!isRegistered(c)) {
            LOGGER.error("Unregistered type for %s", c);
            return;
        }

        outputHelp(out, c);
        out.print("  ");
        outputSignature(out, false, description.getClassName(), c);
        out.println(";");
    }

    private static boolean addParent(HashMap<Class<?>, ClassNode> nodes, ClassNode node, Class<?> aClass) {
        final ClassNode parentNode = nodes.get(aClass);
        if (parentNode != null) {
            parentNode.children.add(node);
            node.parents.add(parentNode);
            return true;
        }
        return false;
    }

    private static void addInterfaces(HashMap<Class<?>, ClassNode> nodes, ClassNode node, Class<?> current) {
        for (Class<?> anInterface : current.getInterfaces()) {
            if (!addParent(nodes, node, anInterface)) {
                // Go up in the hierarchy
                addInterfaces(nodes, node, anInterface);
            }
        }
    }

    private static void outputHelp(PrintStream out, Declaration<?> d) {
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
        final Expose expose = declaration.executable().getAnnotation(Expose.class);
        int optional = (expose != null) ? expose.optional() : 0;

        for (int i = 0; i < n; ++i) {
            final boolean isOptional = i >= n - optional;
            out.format("  params[\"%s\"] = RPCConverter<%s>::toJson(%s);%n", parameterNames[i],
                    cppname(executable.getParameterTypes()[i], isOptional), parameterNames[i]);
        }

        String callName = declaration.isStatic() ? "__static_call__" : "__call__";

        if (executable instanceof Constructor) {
            out.format("  __set__(%s(\"%s.__init__\", params));%n", callName, prefix);
        } else {
            Type returnType = executable.getAnnotatedReturnType().getType();
            if (returnType == Void.TYPE) {
                out.format("  %s(\"%s.%s\", params);%n", callName, prefix, declaration.getName());
            } else {
                out.format("  return RPCConverter<%s>::toCPP(%s(\"%s.%s\", params));%n",
                        cppname(returnType, false),
                        callName, prefix, declaration.getName());
            }
        }
        out.format("}%n%n");
    }

    private static void outputSignature(PrintStream out, boolean definition, String className,
                                        Declaration<?> declaration) {
        final Executable executable = declaration.executable();

        if (executable instanceof Method) {
            if (!definition) {
                if (declaration.isStatic()) {
                    out.print("static ");
                } else {
                    out.print("virtual ");
                }
            }
            out.print(cppname(executable.getAnnotatedReturnType().getType(), false));
            out.print(' ');
        }

        String name = declaration instanceof MethodDeclaration ? declaration.getName() : className;
        if (definition) {
            out.format("%s::%s(", className, name);
        } else {
            out.format("%s(", name);
        }

        final String[] parameterNames = declaration.getParameterNames();

        final Expose expose = declaration.executable().getAnnotation(Expose.class);
        int optional = (expose != null) ? expose.optional() : 0;
        assert expose == null || !expose.optionalsAtStart();

        final int parameterCount = declaration.executable().getParameterCount();
        for (int i = 0; i < parameterCount; ++i) {
            if (i > 0) out.print(", ");
            final boolean isOptional = i >= parameterCount - optional;
            out.format("%s const &%s", cppname(executable.getParameterTypes()[i], isOptional), parameterNames[i]);
            if (!definition && isOptional) {
                out.format(" = %s()", cppname(executable.getParameterTypes()[i], isOptional));
            }
        }
        out.print(")");

        if (declaration.isPureVirtual()) {
            assert !definition;
            out.print(" = 0");
        }
    }

    private static final HashMap<Type, String> type2cppType = new HashMap<>();

    static {
        type2cppType.put(String.class, "std::string");
        type2cppType.put(Void.TYPE, "void");

        type2cppType.put(Boolean.class, "bool");
        type2cppType.put(boolean.class, "bool");
        type2cppType.put(long.class, "int64_t");
        type2cppType.put(Long.class, "int64_t");
        type2cppType.put(Integer.class, "int32_t");
        type2cppType.put(int.class, "int32_t");
        type2cppType.put(Float.TYPE, "float");
        type2cppType.put(Double.TYPE, "double");
    }

    private static boolean isRegistered(Type type) {
        return cppname(type, false) != null;
    }

    private static String cppname(Type type, boolean isOptional) {
        final String s = type2cppType.get(type);
        if (s != null) {
            if (isOptional) {
                return "optional<" + s + ">";
            }
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
            String cppname = cppname(aClass.getComponentType(), false);
            return cppname == null ? null : "std::vector<" + cppname + ">";
        }

        if (List.class.isAssignableFrom(aClass)) {
            final TypeToken<? extends List> wrapperType = TypeToken.of((Class<? extends List>) aClass);

            String cppname = cppname(wrapperType.resolveType(List.class.getTypeParameters()[0]).getType(), isOptional);

            return cppname == null ? null : "std::vector<" + cppname + ">";

        }

        return null;
    }
}
