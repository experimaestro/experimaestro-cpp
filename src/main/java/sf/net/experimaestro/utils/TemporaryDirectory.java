package sf.net.experimaestro.utils;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

/**
 * A temporary directory that is automatically deleted when the JVM stops (or on
 * demand)
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class TemporaryDirectory {
	private static final Logger logger = Logger
			.getLogger(TemporaryDirectory.class);

	Thread deleteThread = null;
	File directory;

	/**
	 * Creates a temporary directory in the default (system) temporary directory
	 * 
	 * @param prefix
	 * @param suffix
	 * @throws IOException
	 */
	public TemporaryDirectory(String prefix, String suffix) throws IOException {
		this(null, prefix, suffix);
	}

	/**
	 * Creates a temporary directory that will be automatically deleted at the
	 * end of the java program
	 * 
	 * @param basedir
	 * @param prefix
	 * @param suffix
	 * @throws IOException
	 */
	public TemporaryDirectory(File basedir, String prefix, String suffix)
			throws IOException {
		// Create a temporary directory (begins with a file, then change to a
		// directory)
		directory = basedir != null ? File.createTempFile(prefix, suffix,
				basedir) : File.createTempFile(prefix, suffix);
		directory.delete();
		deleteThread = new Thread() {
			@Override
			public void run() {
				logger.debug("Deleting temporary directory " + directory);
				if (directory.exists()) {
					FileSystem.recursiveDelete(directory);
				}
			}
		};
		setAutomaticDelete(true);
		directory.mkdir();
		logger.debug("Created temporary directory " + directory);
	}

	public void setAutomaticDelete(boolean status) {
		if (status)
			Runtime.getRuntime().addShutdownHook(deleteThread);
		else
			Runtime.getRuntime().removeShutdownHook(deleteThread);
	}

	/**
	 * Dispose of the directory
	 */
	public void close() {
		if (deleteThread != null) {
			deleteThread.run();
			setAutomaticDelete(false);
		}
	}

	public File getFile() {
		return directory;
	}
}
