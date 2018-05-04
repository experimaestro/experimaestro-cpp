/**
 *
 */
package net.bpiwowar.xpm.manager.tasks;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
 *
 * experimaestro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * experimaestro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import static java.lang.String.format;


/**
 * Methods used for introspection
 *
 * @author B. Piwowarski
 */
public class Introspection {
    public static final String CLASS = ".class";
    final static private Logger LOGGER = Logger.getLogger(Introspection.class.getName());

    /**
     * Get the list of class which implement a given class
     *
     * @param which       The base class
     * @param packageName the package where classes are searched
     * @param levels      number of levels to be explored (-1 for infinity)
     * @return an object of objects of the given class
     */
    static public Class<?>[] getImplementors(final Class<?> which,
                                             final String packageName, final int levels) {
        final ArrayList<Class<?>> list = new ArrayList<Class<?>>();
        Introspection.addImplementors(list, which, packageName, levels);
        final Class<?>[] objects = (Class<?>[]) java.lang.reflect.Array
                .newInstance(Class.class, list.size());
        return list.toArray(objects);
    }

    /**
     * Get the list of class which implement a given class
     *
     * @param cl          The class loader to use
     * @param which       The base class
     * @param packageName the package where classes are searched
     * @param levels      number of levels to be explored (-1 for infinity)
     * @param <T>         The base class
     * @return an array of objects of the given class
     */
    static public <T> Class<? extends T>[] getImplementors(final ClassLoader cl, final Class<T> which,
                                                           final String packageName, final int levels) {
        final ArrayList<Class<? extends T>> list = new ArrayList<>();
        addImplementors(cl, list, which, packageName, levels);
        final Class<? extends T>[] objects = (Class<? extends T>[]) java.lang.reflect.Array
                .newInstance(Class.class, list.size());
        return list.toArray(objects);
    }

    public static <T> void addImplementors(final ClassLoader cl, final Collection<Class<? extends T>> list,
                                           final Class<?> which, final String packageName, final int levels) {
        final ArrayList<Class<?>> aList = new ArrayList<>();
        addClasses(cl, aClass -> which.isAssignableFrom(aClass), aList, packageName, levels);
        AbstractList<Class<? extends T>> transformedList = new AbstractList<Class<? extends T>>() {
            @Override
            public int size() {
                return 0;
            }

            @Override
            public Class<? extends T> get(int index) {
                return (Class<? extends T>) aList.get(index);
            }
        };
        list.addAll(transformedList);
    }

    static public ArrayList<Class<?>> getClasses(final ClassLoader cl, final Checker checker,
                                                 final String packageName, final int levels) {
        final ArrayList<Class<?>> list = new ArrayList<>();
        addClasses(cl, checker, list, packageName, levels);
        return list;
    }

    /**
     * Add classes to the list
     *
     * @param cl          The class loader
     * @param checker     Used to filter the classes
     * @param list        The list to fill
     * @param packageName The package to be analyzed
     * @param levels      The maximum number of recursion within the structure (or -1 if
     *                    infinite)
     * @param <T>         The base class
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
                System.err.println("Exception in introspection: " + ioex);
            }
        }
    }

    public static void addClasses(final ClassLoader cl, final Checker checker,
                                  final ArrayList<Class<?>> list, final int levels,
                                  final JarURLConnection conn, final String prefix)
            throws IOException {
        final JarFile jfile = conn.getJarFile();
        LOGGER.fine(format("Exploring jar file %s with prefix %s", jfile.getName(),
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
                    LOGGER.fine("Testing class " + classname);
                    final Class<?> oclass = Class.forName(classname);
                    if (checker.accepts(oclass))
                        list.add(oclass);
                } catch (final Exception ex) {
                    LOGGER.fine("Caught exception " + ex);
                }
            }
        }
    }

    public static <T> Stream<? extends ClassFile> findClasses(final Path file, final int levels, final String packageName) throws IOException {
        final String name = packageName.replace('.', '/');
        return classesStream(file.resolve(name), levels, packageName);
    }

    /**
     * Returns a stream of classes files (a class name + file) within a package
     *
     * @param file        The file or directory to inspect
     * @param levels      The number of levels of recursion
     * @param packageName The name of the package
     * @return
     */
    private static Stream<? extends ClassFile> classesStream(final Path file, final int levels, final String packageName) throws IOException {
        if (Files.isDirectory(file)) {
            if (levels <= 0) {
                return Stream.empty();
            }

            Stream<? extends ClassFile> base = Stream.empty();
            for (Path child : Files.newDirectoryStream(file)) {
                base = Stream.concat(base, classesStream(child, levels - 1, packageName + "." + child.getFileName().toString()));
            }
            return base;

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
                    // Try to createSSHAgentIdentityRepository an instance of the object
                    try {
                        classname = packageName + "." + classname;
                        final Class<?> oclass = cl.loadClass(classname);
                        if (checker.accepts(oclass))
                            list.add(oclass);
                    } catch (final Throwable e) {
                        LOGGER.warning(format("Could not load class %s: %s", classname, e));
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

    public static void addImplementors(final ArrayList<Class<?>> list,
                                       final Class<?> which, final String packageName, final int levels) {
        addClasses(aClass -> which.isAssignableFrom(aClass), list, packageName, levels);
    }

    static public ArrayList<Class<?>> getClasses(final Checker checker,
                                                 final String packageName, final int levels) {
        final ArrayList<Class<?>> list = new ArrayList<>();
        Introspection.addClasses(checker, list, packageName, levels);
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
    public static void addClasses(final Checker checker,
                                  final ArrayList<Class<?>> list, final String packageName,
                                  final int levels) {
        final String name = "/" + packageName.replace('.', '/');

        // Get a File object for the package
        final URL url = Introspection.class.getResource(name);
        if (url == null)
            return;

        addClasses(checker, list, packageName, levels, url);
    }

    public static void addClasses(final Checker checker,
                                  final ArrayList<Class<?>> list, final String packageName,
                                  final int levels, final URL url) {

        String path;
        try {
            path = URLDecoder.decode(url.getFile(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            path = url.getFile();
        }
        final File directory = new File(path);

        // File directory
        if (directory.exists()) {
            addClasses(checker, list, packageName, levels, directory);
        } else {
            try {
                // It does not work with the filesystem: we must
                // be in the case of a package contained in a jar file.
                final JarURLConnection conn = (JarURLConnection) url
                        .openConnection();
                addClasses(checker, list, levels, conn, packageName.replace(
                        '.', '/'));
            } catch (final IOException ioex) {
                LOGGER.info(format("Exception while introspecting: %s", ioex));
            }
        }
    }

    public static void addClasses(final Checker checker,
                                  final ArrayList<Class<?>> list, final int levels,
                                  final JarURLConnection conn, final String prefix)
            throws IOException {
        final JarFile jfile = conn.getJarFile();

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
                    LOGGER.fine("Testing class " + classname);
                    final Class<?> oclass = Class.forName(classname);
                    if (checker.accepts(oclass))
                        list.add(oclass);
                } catch (final Exception ex) {
                    LOGGER.warning("Caught exception " + ex);
                }
            }
        }
    }

    public static void addClasses(final Checker checker,
                                  final ArrayList<Class<?>> list, final String packageName,
                                  final int levels, final File directory) {
        // Get the list of the files contained in the package
        final File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                // we are only interested in .class files
                if (levels != 0 && file.isDirectory())
                    addClasses(checker, list, packageName + "."
                            + file.getName(), levels - 1);
                if (file.getName().endsWith(".class")) {
                    // removes the .class extension
                    String classname = file.getName().substring(0,
                            file.getName().length() - 6);
                    // Try to create an instance of the object
                    try {
                        classname = packageName + "." + classname;
                        final Class<?> oclass = Class.forName(classname);
                        if (checker.accepts(oclass))
                            list.add(oclass);
                    } catch (final Exception e) {
                    }
                }
            }
        }
    }

    static public class ClassFile {
        public Path file;
        public String classname;

        public ClassFile(Path file, String classname) {
            this.file = file;
            this.classname = classname;
        }
    }
}
