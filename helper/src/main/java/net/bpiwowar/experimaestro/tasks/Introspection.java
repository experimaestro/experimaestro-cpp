/**
 * 
 */
package net.bpiwowar.experimaestro.tasks;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;



/**
 * Methods used for introspection
 * 
 * @author B. Piwowarski
 * 
 */
public class Introspection {
    final static private Logger logger = Logger.getLogger(Introspection.class.getName());

	/**
	 * Get the list of class which implement a given class
	 * 
	 * @param which
	 *            The base class
	 * @param packageName
	 *            the package where classes are searched
	 * @param levels
	 *            number of levels to be explored (-1 for infinity)
	 * @return an array of objects of the given class
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
	 * A checker class
	 * 
	 * @author bpiwowar
	 */
	public interface Checker {
		boolean accepts(Class<?> aClass);
	}

	public static void addImplementors(final ArrayList<Class<?>> list,
			final Class<?> which, final String packageName, final int levels) {
		addClasses(new Checker() {
			public boolean accepts(Class<?> aClass) {
				return which.isAssignableFrom(aClass);
			}

		}, list, packageName, levels);
	}

	static public ArrayList<Class<?>> getClasses(final Checker checker,
			final String packageName, final int levels) {
		final ArrayList<Class<?>> list = new ArrayList<Class<?>>();
		Introspection.addClasses(checker, list, packageName, levels);
		return list;
	}

	/**
	 * Add classes to the list
	 * 
	 * @param checker
	 *            Used to filter the classes
	 * @param list
	 *            The list to fill
	 * @param packageName
	 *            The package to be analyzed
	 * @param levels
	 *            The maximum number of recursion within the structure (or -1 if
	 *            infinite)
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
                System.err.println(ioex);
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
					logger.fine("Testing class " + classname);
					final Class<?> oclass = Class.forName(classname);
					if (checker.accepts(oclass))
						list.add(oclass);
				} catch (final Exception ex) {
					logger.warning("Caught exception " + ex);
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
			for (int i = 0; i < files.length; i++) {

				// we are only interested in .class files
				if (levels != 0 && files[i].isDirectory())
					addClasses(checker, list, packageName + "."
							+ files[i].getName(), levels - 1);
				if (files[i].getName().endsWith(".class")) {
					// removes the .class extension
					String classname = files[i].getName().substring(0,
							files[i].getName().length() - 6);
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

}
