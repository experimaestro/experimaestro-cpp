package sf.net.experimaestro.scheduler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Level;

import sf.net.experimaestro.utils.log.Logger;
import sf.net.experimaestro.utils.GenericHelper;
import sf.net.experimaestro.utils.Heap;
import sf.net.experimaestro.utils.ThreadCount;
import sf.net.experimaestro.utils.iterators.AbstractIterator;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.EnvironmentLockedException;
import com.thoughtworks.xstream.XStream;

/**
 * Thread manager for running commands - it has a pool of runs
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Scheduler {
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
			Job task;
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
			} catch (InterruptedException e) {
				LOGGER.warn("We were interrupted %s", e);
			}

			counter.del();
		}
	}

	/**
	 * This task runner takes a new task each time
	 */
	class ResourceChecker extends TimerTask {
		@Override
		public void run() {
			synchronized (Scheduler.this) {
				boolean changed = false;
				// Update resources status
				for (WeakReference<Resource> wr : resources.values()) {
					Resource resource = wr.get();
					if (resource != null) {
						if (resource.updateStatus()) {
							resource.notifyListeners();

							// Notify the task manager in the case of a task
							if (resource instanceof Job)
								Scheduler.this.updateState((Job) resource);

							changed = true;
						}
					}
				}
				LOGGER.log(changed ? Level.INFO : Level.DEBUG,
						"Checked resources (changes=%b)", changed);
				if (changed)
					Scheduler.this.notify();
			}
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
	public Scheduler(File baseDirectory, int nbThreads)
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

		// Add a timer thread for checking resources (every ten seconds)
		Timer timer = new Timer("check-rsrc");
		timer.schedule(new ResourceChecker(), 10000, 10000);

		LOGGER.info("Done - ready to work now");

	}

	/**
	 * The list of jobs - with those having all dependencies fulfilled first
	 */
	Heap<Job> tasks = new Heap<Job>(TaskComparator.INSTANCE);

	/**
	 * The list of tasks (to find them by id)
	 */
	HashSet<Job> taskSet = GenericHelper.newHashSet();

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
		Resource resource = ref != null ? ref.get() : null;

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
					resources.put(resource.identifier,
							new WeakReference<Resource>(resource));
				} catch (IOException e) {
					resource = null;
					LOGGER.error("Could not load resource %s", id);
				}

			}
		}

		return resource;
	}

	/**
	 * Add a given resource
	 */
	synchronized public void add(Resource task) {
		// --- Notify
		LOGGER.info("Add the resource %s", task);
		resources.put(task.identifier, new WeakReference<Resource>(task));
	}

	/**
	 * Add a given job
	 */
	synchronized public void add(Job task) {
		// --- Notify
		LOGGER.info("Add the task %s", task);

		tasks.add(task);
		taskSet.add(task);

		// Also adds it as a resource
		resources.put(task.identifier, new WeakReference<Resource>(task));

		// --- Notify if a task runner is waiting for a fresh new task
		notify();
	}

	/**
	 * Called when something has changed for this task (in order to update the
	 * heap)
	 */
	synchronized void updateState(Job task) {
		// Update the task and notify ourselves since we might want
		// to run new processes

		// Update the heap
		tasks.update(task);

		// Notify
		notify();
	}

	/**
	 * Execute the next available job
	 * 
	 * @return A boolean, true if a task was started
	 * @throws InterruptedException
	 */
	Job getNextTask() throws InterruptedException {
		while (true) {
			// Try the next task
			synchronized (this) {
				if (!tasks.isEmpty()) {
					final Job task = tasks.peek();
					LOGGER.info(
							"Checking task %s for execution [%d unsatisfied]",
							task, task.nbUnsatisfied);
					if (task.nbUnsatisfied == 0)
						return tasks.pop();
				}

				// ... and wait if we were not lucky (or there were no tasks)
				wait();
			}

		}
	}

	/**
	 * Iterator on resources
	 */
	synchronized public Iterable<Resource> resources() {
		return new Iterable<Resource>() {
			public Iterator<Resource> iterator() {
				return new AbstractIterator<Resource>() {
					Iterator<WeakReference<Resource>> iterator = resources
							.values().iterator();

					@Override
					protected boolean storeNext() {
						while (iterator.hasNext()) {
							if ((value = iterator.next().get()) != null)
								return true;
						}
						return false;
					}
				};
			}
		};
	}

	public Iterable<Job> tasks() {
		return tasks;
	}
}
