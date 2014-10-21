package sf.net.experimaestro.utils.introspection;

import org.apache.commons.vfs2.FileObject;
import sf.net.experimaestro.exceptions.XPMRuntimeException;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import static java.lang.String.format;


/**
 * An object to hold class information. For speed purposes, this is reconstructed directly from the
 * classfile header without calling the classloader.
 * <p/>
 * See
 * http://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html
 */
public class ClassInfo implements AnnotatedElement {
    /**
     * The class info loader
     */
    private final ClassInfoLoader classInfoLoader;

    /**
     * Class name
     */
    String name;

    /**
     * Set to true when this class is encountered in the classpath (false if the class is so far only
     * cited as a superclass)
     */
    boolean loaded;

    /**
     * All interfaces
     */
    HashSet<ClassInfo> interfaces = new HashSet<>();

    /**
     * All annotations
     */
    HashMap<String, AnnotationInfo> annotations = new HashMap<>();

    /**
     * All fields
     */
    HashMap<String, FieldInfo> fields = new HashMap<>();

    /**
     * Is interface
     */
    private boolean isInterface;

    /**
     * Superclass (if any)
     */
    private ClassInfo superclass;

    /**
     * The matching class
     */
    private Class<?> theClass;

    /**
     * This class was encountered on the classpath.
     */
    public ClassInfo(ClassInfoLoader cil, String name) {
        this.classInfoLoader = cil;
        this.name = name;
        this.loaded = false;
    }

    public ClassInfo(ClassInfoLoader classInfoLoader, FileObject file) throws IOException {
        this(classInfoLoader, file.getContent().getInputStream());
    }

    /**
     * Read class information from disk.
     */
    public ClassInfo(ClassInfoLoader classInfoLoader, final InputStream stream) throws IOException {
        this.classInfoLoader = classInfoLoader;
        readFromInputStream(stream);
    }

    private static Object readRefd(DataInputStream inp, Object[] constantPool) throws IOException {
        int constantPoolIdx = inp.readUnsignedShort();
        return constantPool[constantPoolIdx];
    }

    /**
     * Read a string reference from a classfile, then look up the string in the constant pool.
     */
    private static String readRefdString(DataInputStream inp, Object[] constantPool) throws IOException {
        Object constantPoolObj = readRefd(inp, constantPool);
        return (constantPoolObj instanceof Integer ? (String) constantPool[(Integer) constantPoolObj]
                : (String) constantPoolObj);
    }

    public boolean belongs(Class<?> aclass) {
        if (aclass.isPrimitive()) {
            resolve();
            return aclass == theClass;
        }

        if (aclass.getName().equals(this.name))
            return true;

        resolve();
        if (superclass == null)
            return false;

        return superclass.belongs(aclass);
    }

    public String getName() {
        return name;
    }

    /**
     * Directly examine contents of classfile binary header.
     */
    private void readFromInputStream(final InputStream inputStream) throws IOException {
        DataInputStream inp = new DataInputStream(new BufferedInputStream(inputStream, 1024));

        // Magic
        if (inp.readInt() != 0xCAFEBABE) {
            // Not classfile
            throw new IOException("Not a class file");
        }

        // Minor version
        inp.readUnsignedShort();
        // Major version
        inp.readUnsignedShort();

        // Constant pool count (1-indexed, zeroth entry not used)
        int cpCount = inp.readUnsignedShort();
        // Constant pool
        Object[] constantPool = new Object[cpCount];
        for (int i = 1; i < cpCount; ++i) {
            final int tag = inp.readUnsignedByte();
            switch (tag) {
                case 1: // Modified UTF8
                    constantPool[i] = inp.readUTF();
                    break;
                case 3: // int
                    constantPool[i] = inp.readInt();
                    break;
                case 4: // float
                    constantPool[i] = inp.readFloat();
                    break;
                case 5: // long
                    constantPool[i] = inp.readLong();
                    i++;
                    break;
                case 6: // double
                    constantPool[i] = inp.readDouble();
                    i++; // double slot
                    break;
                case 7: // Class
                case 8: // String
                    // Forward or backward reference a Modified UTF8 entry
                    constantPool[i] = inp.readUnsignedShort();
                    break;
                case 9: // field ref
                case 10: // method ref
                case 11: // interface ref
                case 12: // name and type
                    inp.skipBytes(4); // two shorts
                    break;
                case 15: // method handle
                    inp.skipBytes(3);
                    break;
                case 16: // method type
                    inp.skipBytes(2);
                    break;
                case 18: // invoke dynamic
                    inp.skipBytes(4);
                    break;
                default:
                    throw new ClassFormatError("Unkown tag value for constant pool entry: " + tag);
            }
        }

        // Access flags
        int flags = inp.readUnsignedShort();
        isInterface = (flags & 0x0200) != 0;

        // This class name, with slashes replaced with dots
        String name = readRefdString(inp, constantPool).replace('/', '.');
        if (this.name == null) {
            this.name = name;
        } else {
            if (!this.name.equals(name))
                throw new IllegalStateException(format("Class name %s and %s do not match", name, this.name));
        }
        // Superclass name, with slashes replaced with dots
        superclass = classInfoLoader.get(readRefdString(inp, constantPool).replace('/', '.'));

        // Interfaces
        int interfaceCount = inp.readUnsignedShort();
        for (int i = 0; i < interfaceCount; i++) {
            interfaces.add(classInfoLoader.get(readRefdString(inp, constantPool).replace('/', '.')));
        }

        // Fields
        int fieldCount = inp.readUnsignedShort();
        for (int i = 0; i < fieldCount; i++) {
            inp.skipBytes(2); // access_flags
            final String fieldName = readRefdString(inp, constantPool);// name_index,

            String fieldDescriptor = readRefdString(inp, constantPool);// name_index,
            if (fieldDescriptor.startsWith("L") && fieldDescriptor.endsWith(";")) {
                fieldDescriptor = fieldDescriptor.substring(1, fieldDescriptor.length() - 1).replace("/", ".");
            }

            final FieldInfo fieldInfo = new FieldInfo(fieldName, classInfoLoader.get(fieldDescriptor));
            fields.put(fieldName, fieldInfo);

            int attributesCount = inp.readUnsignedShort();
            for (int j = 0; j < attributesCount; j++) {
                String attributeName = readRefdString(inp, constantPool);
                int attributeLength = inp.readInt();
                if ("RuntimeVisibleAnnotations".equals(attributeName)) {
                    int annotationCount = inp.readUnsignedShort();
                    for (int m = 0; m < annotationCount; m++) {
                        AnnotationInfo annotation = readAnnotation(inp, constantPool);
                        fieldInfo.annotations.put(annotation.annotationClass.getName(), annotation);
                    }
                } else {
                    inp.skipBytes(attributeLength);
                }
            }
        }

        // Methods
        int methodCount = inp.readUnsignedShort();
        for (int i = 0; i < methodCount; i++) {
            inp.skipBytes(6); // access_flags, name_index, descriptor_index
            int attributesCount = inp.readUnsignedShort();
            for (int j = 0; j < attributesCount; j++) {
                inp.skipBytes(2); // attribute_name_index
                int attributeLength = inp.readInt();
                inp.skipBytes(attributeLength);
            }
        }

        // Attributes (including class annotations)
        int attributesCount = inp.readUnsignedShort();
        for (int i = 0; i < attributesCount; i++) {
            String attributeName = readRefdString(inp, constantPool);
            int attributeLength = inp.readInt();
            if ("RuntimeVisibleAnnotations".equals(attributeName)) {
                int annotationCount = inp.readUnsignedShort();
                for (int m = 0; m < annotationCount; m++) {
                    AnnotationInfo annotation = readAnnotation(inp, constantPool);
                    annotations.put(annotation.annotationClass.getName(), annotation);
                }
            } else {
                inp.skipBytes(attributeLength);
            }
        }
    }

    /**
     * Read annotation entry from classfile.
     */
    private AnnotationInfo readAnnotation(final DataInputStream inp, Object[] constantPool) throws IOException {
        String annotationFieldDescriptor = readRefdString(inp, constantPool);
        String annotationClassName;
        if (annotationFieldDescriptor.charAt(0) == 'L'
                && annotationFieldDescriptor.charAt(annotationFieldDescriptor.length() - 1) == ';') {
            // Lcom/xyz/Annotation; -> com.xyz.Annotation
            annotationClassName = annotationFieldDescriptor.substring(1,
                    annotationFieldDescriptor.length() - 1).replace('/', '.');
        } else {
            // Should not happen
            annotationClassName = annotationFieldDescriptor;
        }

        AnnotationInfo annotation = new AnnotationInfo(classInfoLoader.get(annotationClassName));

        int numElementValuePairs = inp.readUnsignedShort();
        for (int i = 0; i < numElementValuePairs; i++) {
            String name = readRefdString(inp, constantPool);
            final Object value = readAnnotationElementValue(inp, constantPool);
            annotation.setAttribute(name, value);
        }

        return annotation;
    }

    /**
     * Read annotation element value from classfile.
     */
    private Object readAnnotationElementValue(final DataInputStream inp, Object[] constantPool)
            throws IOException {
        int tag = inp.readUnsignedByte();
        switch (tag) {
            case 'B':
            case 'C':
            case 'D':
            case 'F':
            case 'I':
            case 'J':
            case 'S':
            case 's':
                return readRefdString(inp, constantPool);

            case 'Z': {
                final Object s = readRefd(inp, constantPool);
                return s != null && s instanceof Integer && ((Integer) s).intValue() != 0;
            }
            case 'e':
                // enum_const_value
                inp.skipBytes(4);
                break;
            case 'c':
                // class_info_index
                inp.skipBytes(2);
                break;
            case '@':
                // Complex (nested) annotation
                return readAnnotation(inp, constantPool);
            case '[':
                // array_value
                final int count = inp.readUnsignedShort();
                for (int l = 0; l < count; ++l) {
                    // Nested annotation element value
                    readAnnotationElementValue(inp, constantPool);
                }
                break;
            default:
                throw new ClassFormatError("Invalid annotation element type tag: 0x" + Integer.toHexString(tag));
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }


    /**
     * Read the full class information
     *
     * @return The object
     */
    public ClassInfo resolve() {
        if (loaded) {
            return this;
        }

        if (isArray()) {
            loaded = true;
            return this;
        }

        // Simple cases
        switch (name) {
            case "B":
                theClass = Byte.TYPE;
                break;
            case "C":
                theClass = Character.TYPE;
                break;
            case "D":
                theClass = Double.TYPE;
                break;
            case "F":
                theClass = Float.TYPE;
                break;
            case "I":
                theClass = Integer.TYPE;
                break;
            case "J":
                theClass = Long.TYPE;
                break;
            case "S":
                theClass = Short.TYPE;
                break;
            case "Z":
                theClass = Boolean.TYPE;
                break;
            default:
                final InputStream stream = classInfoLoader.getStream(name);
                if (stream != null) {
                    try {
                        readFromInputStream(stream);
                    } catch (IOException e) {
                        throw new XPMRuntimeException(e, "Could not read class %s", name);
                    }
                } else {
                    try {
                        theClass = classInfoLoader.classLoader.loadClass(name);
                    } catch (ClassNotFoundException e) {
                        throw new XPMRuntimeException(e, "Could not find class %s", name);
                    }
                }
        }

        loaded = true;
        return this;
    }


    public boolean isInterface() {
        return isInterface;
    }


    @Override
    public AnnotationInfo getAnnotationInfo(Class<?> annotationClass) {
        return annotations.get(annotationClass.getName());
    }

    public Collection<FieldInfo> getDeclaredFields() {
        return fields.values();
    }

    public boolean isArray() {
        if (theClass != null)
            return theClass.isArray();

        return name.startsWith("[");
    }

    public ClassInfo getComponentType() {
        if (!isArray())
            return null;

        return classInfoLoader.get(name.substring(1));
    }

}
