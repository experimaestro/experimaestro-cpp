package bpiwowar.expmanager.rsrc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import bpiwowar.argparser.EnumValue;
import bpiwowar.argparser.ReadLineIterator;
import bpiwowar.expmanager.utils.WatchFileMonitor;
import bpiwowar.log.Logger;

/**
 * 
 * A simple piece of data
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class SimpleData extends Data {
	final static private Logger LOGGER = Logger.getLogger();

	/**
	 * The access mode
	 */
	private final Mode mode;

	/**
	 * Locking mode
	 */

	public enum Mode {
		@EnumValue(value = "read-only", help = "Creates a resource that can only be read")
		READ_ONLY,

		@EnumValue(value = "exclusive-write", help = "Creates a resource that can be read by many, but need exclusive write access")
		EXCLUSIVE_WRITE,

		@EnumValue(value = "single-writer", help = "Creates a resource that can be read by many, and can be written by at most one")
		SINGLE_WRITER,

		@EnumValue(value = "multiple-writers", help = "Creates a resource that can be read by many, but can write access")
		MULTIPLE_WRITERS,
	}

	public SimpleData(TaskManager taskManager, String identifier, Mode mode) {
		super(taskManager, identifier);
		this.mode = mode;
	}

	protected class MyLock implements Lock {
		public void dispose() throws IOException {
			File lockFile = new File(identifier + ".lock");
			lock(lockFile);
			try {
				
			} finally {
				lockFile.delete();
			}
		}

		@Override
		protected void finalize() throws Throwable {
			dispose();
		}

	}

	void lock(File lockFile) throws IOException {
		WatchFileMonitor monitor = new WatchFileMonitor(lockFile,
				WatchFileMonitor.Mode.DELETED);

		while (!lockFile.createNewFile())
			monitor.take();
	}

	/**
	 * Tries to lock the resource
	 * 
	 * @param write
	 * @throws IOException
	 */
	public Lock lock(boolean write) throws IOException {
		File lockFile = new File(identifier + ".lock");
		lock(lockFile);

		// (1) We get a lock on the resource
		try {
			// (2) We read the current state
			readStatus();

			// (3) Add us
			if (!isReady())
				return null;

			File status = new File(identifier + ".status");
			PrintStream out = new PrintStream(
					new FileOutputStream(status, true));
			out.format("%d %s%n", bpiwowar.expmanager.utils.Process.getPID(),
					write ? "w" : "r");
			out.close();

		} finally {
			// Remove the lock
			lockFile.delete();
		}
		return null;
	}

	/**
	 * Update the current status (can be thread safe or not)
	 * 
	 * @throws FileNotFoundException
	 */
	private void readStatus() throws FileNotFoundException {
		File statusFile = new File(identifier + ".status");
		if (!statusFile.exists()) {
			writers = readers = 0;
		} else {
			// Read the status
			for (String line : new ReadLineIterator(statusFile)) {
				String[] fields = line.split("\\s+");
				if (fields.length != 2)
					LOGGER.error("Skipping line %s (wrong number of fields)",
							line);
				else {
					if (fields[1].equals("r"))
						readers += 1;
					else if (fields[1].equals("w"))
						writers += 1;
					else if (fields[1].equals("rw")) {
						readers += 1;
						writers += 1;
					} else
						LOGGER.error("Skipping line %s (unkown mode %s)",
								fields[1]);
				}
			}

		}
	}

	/** Number of readers and writers */
	int writers, readers;

	@Override
	public boolean isReady(DependencyType type) {
		switch (type) {
		case GENERATED:
			return isReady();

		case READ_ACCESS:
			return isReady();

		case WRITE_ACCESS:
			return mode != Mode.READ_ONLY && isReady();
		}
		return false;
	}

}
