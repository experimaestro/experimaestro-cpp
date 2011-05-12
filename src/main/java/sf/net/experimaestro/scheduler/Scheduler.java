package sf.net.experimaestro.scheduler;

import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;
import org.apache.log4j.Level;

import sf.net.experimaestro.exceptions.ExperimaestroException;
import sf.net.experimaestro.utils.Heap;
import sf.net.experimaestro.utils.ThreadCount;
import sf.net.experimaestro.utils.je.FileProxy;
import sf.net.experimaestro.utils.log.Logger;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.EnvironmentLockedException;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.model.AnnotationModel;
import com.sleepycat.persist.model.EntityModel;

/**
 * Thread manager for running commands - it has a pool of runs
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
/**
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * 
 */
public class Scheduler {
	final static private Logger LOGGER = Logger.getLogger();

	public static FileSystemManager fsManager;
	static {
		try {
			fsManager = VFS.getManager();
		} catch (FileSystemException e) {
			throw new ExperimaestroException("Cannot start the VFS manager", e);
		}
	}

	/**
	 * Used for atomic series of locks
	 */
	public static String LockSync = "I am just a placeholder";

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
	 * The list of jobs organised in a heap - with those having all dependencies
	 * fulfilled first
	 */
	Heap<Job> waitingJobs = new Heap<Job>(JobComparator.INSTANCE);

	/**
	 * All the resources
	 */
	Resources resources;

	/**
	 * The database store
	 */
	private EntityStore dbStore;

	/**
	 * The database environement
	 */
	private Environment dbEnvironment;

	/**
	 * This task runner takes a new task each time
	 */
	class JobRunner extends Thread {
		@Override
		public void run() {
			Job job;
			try {
				while (!Scheduler.this.isStopping() && (job = getNextWaitingJob()) != null) {
					LOGGER.info("Starting %s", job);
					try {
						job.run();
						LOGGER.info("Finished %s", job);
					} catch (Throwable t) {
						LOGGER.warn("Houston, we got a problem: %s", t);
					}
				}
			} catch (InterruptedException e) {
				LOGGER.warn("Shutting down job runner", e);
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

				for (Resource resource : resources) {
					// Update resources status
					if (!resource.getListeners().isEmpty())
						if (resource.updateStatus()) {
							try {
								resource.notifyListeners();
							} catch (DatabaseException e) {
								LOGGER.warn(
										"Could not notify all the listeners for [%s]",
										resource.getIdentifier());
								LOGGER.warn("Trace:", e);
							}

							// Notify the task manager in the case of a task
							if (resource instanceof Job)
								Scheduler.this.updateState((Job) resource);

							// Update DB
							try {
								Scheduler.this.store(resource);
							} catch (DatabaseException e) {
								LOGGER.error(
										"Could not update resource %s in database",
										resource);
							}

							changed = true;
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

		// Initialise the JE database
		LOGGER.info("Initialising JE database in directory %s", baseDirectory);
		EnvironmentConfig myEnvConfig = new EnvironmentConfig();
		myEnvConfig.setTransactional(false);
		StoreConfig storeConfig = new StoreConfig();

		myEnvConfig.setAllowCreate(true);
		storeConfig.setAllowCreate(true);
		dbEnvironment = new Environment(baseDirectory, myEnvConfig);

		EntityModel model = new AnnotationModel();
		model.registerClass(FileProxy.class);
		storeConfig.setModel(model);
		dbStore = new EntityStore(dbEnvironment, "SchedulerStore", storeConfig);

		// Add a shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				Scheduler.this.close();
			}
		}));

		// Initialise the store
		resources = new Resources(this, dbStore);

		// Start the threads
		LOGGER.info("Starting %d threads", nbThreads);
		for (int i = 0; i < nbThreads; i++) {
			counter.add();
			final JobRunner runner = new JobRunner();
			threads.add(runner);
			runner.start();
		}

		resourceCheckTimer = new Timer("check-rsrc");
		resourceCheckTimer.schedule(new ResourceChecker(), 10000, 10000);
		LOGGER.info("Done - ready to work now");
	}

	boolean stopping = false;
	protected boolean isStopping() {
		return stopping;
	}

	// List of threads we started
	ArrayList<Thread> threads = new ArrayList<Thread>();

	private Timer resourceCheckTimer;
	
	/**
	 * Shutdown the scheduler
	 */
	public void close() {
		// Stop the checker
		if (resourceCheckTimer != null) {
			resourceCheckTimer.cancel();	
			resourceCheckTimer = null;
		}
		
		// Stop the threads
		for(Thread thread: threads) {
			thread.interrupt();
		}
		threads.clear();
		
		if (dbStore != null) {
			try {
				dbStore.close();
				dbStore = null;
				LOGGER.info("Closed the database store");
			} catch (DatabaseException dbe) {
				LOGGER.error("Error closing MyDbEnv: " + dbe.toString());
				return;
			}
		}

		if (dbEnvironment != null) {
			try {
				// Finally, close environment.
				dbEnvironment.close();
				dbEnvironment = null;
				LOGGER.info("Closed the database environment");
			} catch (DatabaseException dbe) {
				LOGGER.error("Error closing MyDbEnv: " + dbe.toString());
			}
		}
	}

	// ----
	// ---- Task related methods
	// ----

	/**
	 * Get a resource by identifier
	 * 
	 * First checks if the resource is in the list of tasks to be run. If not,
	 * we look directly on disk to get back information on the resource.
	 * 
	 * @throws DatabaseException
	 * 
	 */
	synchronized public Resource getResource(String id)
			throws DatabaseException {
		Resource resource = resources.get(id);
		return resource;
	}

	/**
	 * Add resources
	 * 
	 * @throws DatabaseException
	 */
	synchronized public void add(Resource... list) throws DatabaseException {
		// Loop over the new jobs
		for (Resource resource : list) {
			LOGGER.info("Adding the resource [%s]", resource);

			// First, add the job as a new resource
			resources.put(resource);

			if (resource instanceof Job) {
				Job job = (Job)resource;
				// Add to the list of waiting jobs
				waitingJobs.add(job);

				// Notify the job that it has been added
				job.notify(null);
			}
		}

		// Notify if a task runner is waiting for fresh new task(s)
		notify();
	}

	/**
	 * Called when something has changed for this task (in order to update the
	 * heap)
	 */
	synchronized void updateState(Job job) {
		// Update the task and notify ourselves since we might want
		// to run new processes

		// Update the heap
		waitingJobs.update(job);

		// Notify
		notify();
	}

	/**
	 * Get the next waiting job
	 * 
	 * @return A boolean, true if a task was started
	 * @throws InterruptedException
	 */
	Job getNextWaitingJob() throws InterruptedException {
		while (true) {
			// Try the next task
			synchronized (this) {
				if (!waitingJobs.isEmpty()) {
					final Job task = waitingJobs.peek();
					LOGGER.info(
							"Checking task %s for execution [%d unsatisfied]",
							task, task.nbUnsatisfied);
					if (task.nbUnsatisfied == 0) {
						return waitingJobs.pop();
					}
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
		return resources;
	}

	public Iterable<Job> tasks() {
		return waitingJobs;
	}

	protected void store(Resource resource) throws DatabaseException {
		resources.put(resource);
	}

	public static FileObject resolveFile(String path) {
		try {
			return fsManager.resolveFile(path);
		} catch (FileSystemException e) {
			throw new ExperimaestroException(e, "Cannot resolve path %s", path);
		}
	}
}
