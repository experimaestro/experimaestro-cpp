/**
 *
 */
package sf.net.experimaestro.utils;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.log4j.Logger;
import sf.net.experimaestro.exceptions.XPMRuntimeException;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import static com.google.common.collect.Lists.transform;
import static java.lang.String.format;


/**
 * Methods used for introspection
 *
 * @author B. Piwowarski
 */
public class Introspection {
    public static final String CLASS = ".class";
    public static Logger LOGGER = Logger.getLogger(Introspection.class);

    /**
     * Get the list of class which implement a given class
     *
     * @param cl          The class loader to use
     * @param which       The base class
     * @param packageName the package where classes are searched
     * @param levels      number of levels to be explored (-1 for infinity)
     * @return an array of objects of the given class
     */
    static public <T> Class<? extends T>[] getImplementors(final ClassLoader cl, final Class<T> which,
                                                           final String packageName, final int levels) {
        final ArrayList<Class<? extends T>> list = new ArrayList<>();
        Introspection.addImplementors(cl, list, which, packageName, levels);
        final Class<? extends T>[] objects = (Class<? extends T>[]) java.lang.reflect.Array
                .newInstance(Class.class, list.size());
        return list.toArray(objects);
    }

    public static <T> void addImplementors(final ClassLoader cl, final Collection<Class<? extends T>> list,
                                           final Class<?> which, final String packageName, final int levels) {
        final ArrayList<Class<?>> aList = new ArrayList<>();
        addClasses(cl, aClass -> which.isAssignableFrom(aClass), aList, packageName, levels);
        list.addAll(transform(aList, t -> (Class<? extends T>) t));
    }

    static public ArrayList<Class<?>> getClasses(final ClassLoader cl, final Checker checker,
                                                 final String packageName, final int levels) {
        final ArrayList<Class<?>> list = new ArrayList<>();
        Introspection.addClasses(cl, checker, list, packageName, levels);
        return list;
    }

    /**
     * Add classes to the list
     *
     * @param checker     Used to filter the classes
     * @param list        The list to fill
     * @param packageName The package to be analyzed
     * @param levels      The maximum number of recursion within the structure (or -1 if
     *                    infinite)
     */
    public static <T> void addClasses(final ClassLoader cl, final Checker checker,
                                      final ArrayList<Class<?>> list, final String packageName,
                                      final int levels) {
        final String name = packageName.replace('.', '/');

        // Get a File object for the package
        final URL url = cl.getResource(name);
        if (url == null)
            return;

        addClasses(cl, checker, list, packageName, levels, url);
    }

    public static void addClasses(final ClassLoader cl, final Checker checker,
                                  final ArrayList<Class<?>> list, final String packageName,
                                  final int levels, final URL url) {

        final File directory = new File(url.getFile());

        // File directory
        if (directory.exists()) {
            addClasses(cl, checker, list, packageName, levels, directory);
        } else {
            try {
                // It does not work with the filesystem: we must
                // be in the case of a package contained in a jar file.
                final JarURLConnection conn = (JarURLConnection) url
                        .openConnection();
                addClasses(cl, checker, list, levels, conn, packageName.replace(
                        '.', '/'));
            } catch (final IOException ioex) {
                System.err.println(ioex);
            }
        }
    }

    public static void addClasses(final ClassLoader cl, final Checker checker,
                                  final ArrayList<Class<?>> list, final int levels,
                                  final JarURLConnection conn, final String prefix)
            throws IOException {
        final JarFile jfile = conn.getJarFile();
        LOGGER.debug(Lazy.format("Exploring jar file %s with prefix %s", jfile.getName(),
                prefix));

        final Enumeration<?> e = jfile.entries();
        while (e.hasMoreElements()) {
            final ZipEntry entry = (ZipEntry) e.nextElement();
            final String entryname = entry.getName();

            if (entryname.startsWith(prefix) && entryname.endsWith(".class")) {
                // Check the number of levels
                if (levels >= 0) {
                    int n = 0;
                    for (int i = prefix.length() + 1; i < entryname.length()
                            && n <= levels; i++)
                        if (entryname.charAt(i) == '/')
                            n++;
                    if (n > levels)
                        continue;
                }

                String classname = entryname.substring(0,
                        entryname.length() - 6);
                if (classname.startsWith("/"))
                    classname = classname.substring(1);
                classname = classname.replace('/', '.');
                try {
                    LOGGER.debug("Testing class " + classname);
                    final Class<?> oclass = Class.forName(classname);
                    if (checker.accepts(oclass))
                        list.add(oclass);
                } catch (final Exception ex) {
                    LOGGER.debug("Caught exception " + ex);
                }
            }
        }
    }

    public static <T> Stream<? extends ClassFile> findClasses(final FileObject file, final int levels, final String packageName) throws FileSystemException {
        final String name = packageName.replace('.', '/');
        return classesStream(file.resolveFile(name), levels, packageName);
    }

    /**
     * Returns a stream of classes files (a class name + file) within a package
     *
     * @param file        The file or directory to inspect
     * @param levels      The number of levels of recursion
     * @param packageName The name of the package
     * @return
     */
    private static Stream<? extends ClassFile> classesStream(final FileObject file, final int levels, final String packageName) throws FileSystemException {
        if (file.getType().hasChildren()) {
            if (levels <= 0) {
                return Stream.empty();
            }

            return Arrays.stream(file.getChildren()).flatMap(child -> {
                try {
                    return classesStream(child, levels - 1, packageName + "." + child.getName().getBaseName());
                } catch (FileSystemException e) {
                    throw new XPMRuntimeException();
                }
            });
        }

        if (packageName.endsWith(CLASS)) {
            return Stream.of(new ClassFile(file, packageName.substring(0, packageName.length() - CLASS.length())));
        }

        return Stream.empty();

    }

    public static void addClasses(final ClassLoader cl, final Checker checker,
                                  final ArrayList<Class<?>> list, final String packageName,
                                  final int levels, final File directory) {
        // Get the list of the files contained in the package
        final File[] files = directory.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {

                // we are only interested in .class files
                if (levels != 0 && files[i].isDirectory())
                    addClasses(cl, checker, list, packageName + "."
                            + files[i].getName(), levels - 1);
                if (files[i].getName().endsWith(".class")) {
                    // removes the .class extension
                    String classname = files[i].getName().substring(0,
                            files[i].getName().length() - 6);
                    // Try to create an instance of the object
                    try {
                        classname = packageName + "." + classname;
                        final Class<?> oclass = cl.loadClass(classname);
                        if (checker.accepts(oclass))
                            list.add(oclass);
                    } catch (final Throwable e) {
                        LOGGER.error(format("Could not load class %s", classname), e);
                    }
                }
            }
        }
    }

    /**
     * A checker class
     *
     * @author bpiwowar
     */
    public interface Checker {
        boolean accepts(Class<?> aClass);
    }

    static public class ClassFile {
        public FileObject file;
        public String classname;

        public ClassFile(FileObject file, String classname) {
            this.file = file;
            this.classname = classname;
        }
    }

}
