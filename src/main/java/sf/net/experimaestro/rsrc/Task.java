package sf.net.experimaestro.rsrc;

import static java.lang.String.format;

import java.io.File;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import sf.net.experimaestro.locks.FileLock;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.locks.LockType;
import sf.net.experimaestro.locks.UnlockableException;
import sf.net.experimaestro.utils.HeapElement;
import sf.net.experimaestro.utils.PID;
import sf.net.experimaestro.utils.log.Logger;


/**
 * A task is a resource that can be run - that starts and ends (which
 * differentiate it with a server) and generate data
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Task extends Resource implements HeapElement<Task>, Runnable {
	final static private Logger LOGGER = Logger.getLogger();

	/**
	 * Initialisation of a task
	 * 
	 * @param taskManager
	 */
	public Task(TaskManager taskManager, String identifier) {
		super(taskManager, identifier, LockMode.EXCLUSIVE_WRITER);
	}

	/**
	 * The priority of the job (the higher, the more urgent)
	 */
	int priority;

	/**
	 * When was the job submitted (in case the priority is not enough)
	 */
	long timestamp = System.currentTimeMillis();

	/**
	 * What is the status of a dependency This class stores the previous status
	 * (satisfied or not) in order to update the number of blocking resources
	 */
	static public class DependencyStatusCache {
		LockType type = null;
		boolean isSatisfied = false;

		public DependencyStatusCache(LockType type, boolean isSatisfied) {
			super();
			this.type = type;
			this.isSatisfied = isSatisfied;
		}
	}

	/**
	 * The dependencies for this job (dependencies are on any resource)
	 */
	private SortedMap<Resource, DependencyStatusCache> dependencies = new TreeMap<Resource, DependencyStatusCache>();

	/**
	 * Number of unsatisfied dependencies
	 */
	int nbUnsatisfied;

	/**
	 * Add a dependency
	 * 
	 * @param data
	 *            The data we depend upon
	 */
	public void addDependency(Resource resource, LockType type) {

		LOGGER.info("Adding dependency %s to %s for %s", type, resource, this);
		final DependencyStatus accept = resource.accept(type);
		if (accept == DependencyStatus.ERROR)
			throw new RuntimeException(format(
					"Resource %s cannot be satisfied for lock type %s",
					resource, type));

		resource.register(this);

		final boolean ready = accept.isOK();

		synchronized (this) {
			if (!ready)
				nbUnsatisfied++;
			dependencies.put(resource, new DependencyStatusCache(type, ready));
		}
	}

	@Override
	protected void finalize() throws Throwable {
		for (Resource resource : dependencies.keySet())
			resource.unregister(this);
	}

	/**
	 * Task priority - the higher, the better
	 * 
	 * @param priority
	 *            the priority to set
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}

	/**
	 * @return the priority
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 * This is where the real job gets done
	 * 
	 * @param locks
	 *            The set of locks that were taken
	 * 
	 * @return The error code (0 if everything went fine)
	 * @throws Throwable
	 */
	protected int doRun(ArrayList<Lock> locks) throws Throwable {
		return 1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	final public void run() {
		// Check if the task has already been done
		File doneFile = new File(identifier + ".done");
		if (doneFile.exists()) {
			LOGGER.info("Task %s is already done", identifier);
			return;
		}

		// Our locks
		ArrayList<Lock> locks = new ArrayList<Lock>();

		try {
			while (true) {
				// Check if not done
				if (doneFile.exists()) {
					LOGGER.info("Task %s is already done", identifier);
					lockfile.delete();
					return;
				}

				// Try to lock, otherwise wait
				try {
					locks.add(new FileLock(lockfile, true));
				} catch (UnlockableException e) {
					synchronized (this) {
						try {
							// Wait five seconds before looking again
							wait(5000);
						} catch (InterruptedException ee) {
						}
					}
					continue;
				}

				// Check if not done (again, but now we have a lock so we
				// will be sure of the result)
				if (doneFile.exists()) {
					LOGGER.info("Task %s is already done", identifier);
					lockfile.delete();
					return;
				}

				lockfile.deleteOnExit();
				int pid = PID.getPID();

				// Now, tries to lock all the resources
				for (Entry<Resource, DependencyStatusCache> dependency : dependencies
						.entrySet()) {
					Resource rsrc = dependency.getKey();
					final Lock lock = rsrc
							.lock(pid, dependency.getValue().type);
					if (lock != null)
						locks.add(lock);
				}

				// And run!
				LOGGER.info("Running task %s", identifier);
				try {
					// Run the task
					int code = doRun(locks);

					if (code != 0)
						throw new RuntimeException(String.format(
								"Error while running the task (code %d)", code));

					// Create the "done" file
					LOGGER.info("Done");
					doneFile.createNewFile();
					generated = true;
					notifyListeners();
				} catch (Throwable e) {
					LOGGER.warn("Error while running: %s", e);
				} finally {
					// Dispose of the locks we adquired
					for (Lock lock : locks)
						lock.dispose();
				}

				break;
			}
		} catch (UnlockableException e) {
			throw new RuntimeException(e);
		} finally {
			// Dispose of all locks
			for (Lock lock : locks)
				lock.dispose();
		}

	}

	/**
	 * Called when a resource status has changed
	 * 
	 * @param resource
	 *            The resource has changed
	 * @param objects
	 */
	synchronized public void notify(Resource resource, Object... objects) {
		// Get the status
		DependencyStatusCache status = dependencies.get(resource);

		int k = resource.accept(status.type).isOK() ? 1 : 0;
		final int diff = (status.isSatisfied ? 1 : 0) - k;

		LOGGER.info("Got a notification from %s [%d/%d]", resource, k, diff);

		// Update
		nbUnsatisfied += diff;
		if (k == 1)
			status.isSatisfied = true;
		if (diff != 0)
			taskManager.updateState(this);
	}

	// ----- Heap part (do not touch) -----

	private int index;

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}
}
