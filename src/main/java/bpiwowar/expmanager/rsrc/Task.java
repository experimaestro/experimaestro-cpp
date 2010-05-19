package bpiwowar.expmanager.rsrc;

import java.io.File;
import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;

import bpiwowar.log.Logger;
import bpiwowar.utils.HeapElement;

/**
 * A task is a resource that can be run- that starts and ends (which
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
		super(taskManager, identifier);
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
	static public class DependencyStatus {
		DependencyType type = null;
		boolean isSatisfied = false;

		public DependencyStatus(DependencyType type, boolean isSatisfied) {
			super();
			this.type = type;
			this.isSatisfied = isSatisfied;
		}
	}

	/**
	 * The dependencies for this job (dependencies are on any resource)
	 */
	private SortedMap<Resource, DependencyStatus> dependencies = new TreeMap<Resource, DependencyStatus>();

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
	public void addDependency(Resource resource, DependencyType type) {
		resource.register(this);
		final boolean ready = resource.isReady(type);
		synchronized (this) {
			if (!ready)
				nbUnsatisfied++;
			dependencies.put(resource, new DependencyStatus(type, ready));
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
	 * @throws Throwable
	 */
	protected int doRun() throws Throwable {
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

		// Lock
		File lockFile = new File(identifier + ".lock");
		try {
			while (true) {
				// Check if not done
				if (doneFile.exists()) {
					LOGGER.info("Task %s is already done", identifier);
					lockFile.delete();
					return;
				}

				if (lockFile.createNewFile()) {
					// Check if not done
					if (doneFile.exists()) {
						LOGGER.info("Task %s is already done", identifier);
						lockFile.delete();
						return;
					}

					lockFile.deleteOnExit();

					LOGGER.info("Running task %s", identifier);
					try {
						// Run the task
						int code = doRun();

						if (code != 0)
							throw new RuntimeException(String.format(
									"Error while running the task (code %d)",
									code));

						// Create the "done" file
						LOGGER.info("Done");
						doneFile.createNewFile();
						generated = true;
					} catch (Throwable e) {
						LOGGER.warn("Error while running: %s", e);
					} finally {
						lockFile.delete();
					}

					break;
				} else {
					synchronized (this) {
						try {
							// Wait five seconds before looking again
							wait(5000);
						} catch (InterruptedException e) {
						}
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Notify that resource has changed
	 * 
	 * @param message
	 * @param objects
	 */
	synchronized public void notify(Resource resource, Object... objects) {
		// Get the status
		DependencyStatus status = dependencies.get(resource);

		int k = resource.isReady(status.type) ? 1 : 0;
		final int diff = k - (status.isSatisfied ? 1 : 0);

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

	@Override
	public boolean isReady(DependencyType type) {
		if (type != DependencyType.GENERATED)
			throw new RuntimeException(
					"A task dependency can only be on its completion");

		return generated;
	}

}
