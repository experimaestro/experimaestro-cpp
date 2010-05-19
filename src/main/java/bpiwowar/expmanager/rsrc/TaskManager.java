package bpiwowar.expmanager.rsrc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import bpiwowar.log.Logger;
import bpiwowar.utils.GenericHelper;
import bpiwowar.utils.Heap;
import bpiwowar.utils.ThreadCount;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.EnvironmentLockedException;
import com.thoughtworks.xstream.XStream;

/**
 * Thread manager for running commands - it has a pool of runs
 * 
 * @author bpiwowar
 */
public class TaskManager {
	final static private Logger LOGGER = Logger.getLogger();

	/**
	 * Extension for the serialised version of the task
	 */
	private static final String EXT_INFO = ".info";

	/**
	 * Main directory for the task manager. One database subfolder will be
	 * created
	 */
	File baseDirectory;

	/**
	 * Number of threads running concurrently (excluding any server)
	 */
	int nbThreads = 5;

	/**
	 * Number of running threads
	 */
	ThreadCount counter = new ThreadCount();

	/**
	 * This task runner takes a new task each time
	 */
	class TaskRunner extends Thread {
		@Override
		public void run() {
			Task task;
			try {
				while ((task = getNextTask()) != null) {
					LOGGER.info("Starting %s", task);
					try {
						task.run();
						LOGGER.info("Finished %s", task);
					} catch (Throwable t) {
						LOGGER.warn("Houston, we had a problem: %s", t);
					}
				}
				resources.remove(task);
			} catch (InterruptedException e) {
				LOGGER.warn("We were interrupted %s", e);
			}

			counter.del();
		}

	}

	/**
	 * Initialise the task manager
	 * 
	 * @param baseDirectory
	 * @param nbThreads
	 * @throws EnvironmentLockedException
	 * @throws DatabaseException
	 */
	public TaskManager(File baseDirectory, int nbThreads)
			throws EnvironmentLockedException, DatabaseException {
		// Get the parameters
		this.baseDirectory = baseDirectory;
		this.nbThreads = nbThreads;

		// Start the threads
		LOGGER.info("Starting %d threads", nbThreads);
		for (int i = 0; i < nbThreads; i++) {
			counter.add();
			new TaskRunner().start();
		}

		LOGGER.info("Done - ready to work now");

	}

	/**
	 * The list of jobs - with those having all dependencies fulfilled first
	 */
	Heap<Task> tasks = new Heap<Task>(TaskComparator.INSTANCE);

	/**
	 * The list of tasks (to find them by id)
	 */
	HashSet<Task> taskSet = GenericHelper.newHashSet();

	/**
	 * Cache for resources
	 */
	Map<String, WeakReference<Resource>> resources = Collections
			.synchronizedMap(new HashMap<String, WeakReference<Resource>>());

	// ----
	// ---- Task related methods
	// ----

	/**
	 * Get a resource by identifier
	 * 
	 * First checks if the resource is in the list of tasks to be run. If not,
	 * we look directly on disk to get back information on the resource.
	 * 
	 */
	synchronized public Resource getResource(String id) {
		WeakReference<Resource> ref = resources.get(id);
		Resource resource = ref.get();

		if (resource == null) {
			// Try to get it from disk
			File file = new File(id + EXT_INFO);

			final XStream xstream = new XStream();
			xstream.processAnnotations(Resource.class);
			if (file.exists()) {
				try {
					FileInputStream is = new FileInputStream(file);
					resource = (Resource) new XStream().fromXML(is);
					is.close();
					resources.put(resource.identifier, new WeakReference<Resource>(
							resource));
				} catch (IOException e) {
					resource = null;
					LOGGER.error("Could not load resource %s", id);
				}

			}
		}

		return resource;
	}

	/**
	 * Add a given job
	 */
	synchronized public void add(Task task) {
		// --- Notify
		LOGGER.info("Add the task %s", task);
		tasks.add(task);
		taskSet.add(task);

		// --- Notify if a task runner is waiting for a fresh new task
		notify();
	}

	/**
	 * Called when something has changed for this task (in order to update the
	 * heap)
	 */
	synchronized void updateState(Task task) {
		// Update the task and notify ourselves since
		tasks.update(task);
		notify();
	}

	/**
	 * Execute the next available job
	 * 
	 * @return A boolean, true if a task was started
	 * @throws InterruptedException
	 */
	Task getNextTask() throws InterruptedException {
		while (true) {
			// Try the next task
			synchronized (this) {
				if (!tasks.isEmpty()) {
					final Task task = tasks.peek();
					if (task.nbUnsatisfied == 0)
						return tasks.pop();
				}

				// ... and wait if we were not lucky (or there were no tasks)
				wait();
			}

		}
	}
}
