package sf.net.experimaestro.scheduler;

import static java.lang.String.format;

import java.io.File;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map.Entry;
import java.util.TreeMap;

import sf.net.experimaestro.locks.FileLock;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.locks.LockType;
import sf.net.experimaestro.locks.UnlockableException;
import sf.net.experimaestro.server.XPMServlet;
import sf.net.experimaestro.utils.HeapElement;
import sf.net.experimaestro.utils.PID;
import sf.net.experimaestro.utils.Time;
import sf.net.experimaestro.utils.log.Logger;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.model.Persistent;

/**
 * A job is a resource that can be run - that starts and ends (which
 * differentiate it with a server) and generate data
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Persistent()
public class Job extends Resource implements HeapElement<Job>, Runnable {
	final static private Logger LOGGER = Logger.getLogger();

	protected Job() {
	}

	/**
	 * Initialisation of a task
	 * 
	 * @param taskManager
	 */
	public Job(Scheduler taskManager, String identifier) {
		super(taskManager, identifier, LockMode.EXCLUSIVE_WRITER);
		state = ResourceState.WAITING;
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
	 * When did the job start and stop
	 */
	private long startTimestamp;

	long endTimestamp;

	@Override
	protected boolean isActive() {
		return super.isActive() || state == ResourceState.WAITING
				|| state == ResourceState.RUNNING;
	}

	/**
	 * The dependencies for this job (dependencies are on any resource)
	 */
	private TreeMap<String, Dependency> dependencies = new TreeMap<String, Dependency>();

	/**
	 * Number of unsatisfied dependencies
	 */
	int nbUnsatisfied;

	public TreeMap<String, Dependency> getDependencies() {
		return dependencies;
	}

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
			dependencies.put(resource.getIdentifier(), new Dependency(type,
					ready));
		}
	}

	@Override
	protected void finalize() throws Throwable {
		for (String id : dependencies.keySet()) {
			scheduler.getResource(id).unregister(this);
		}
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
				// in order to avoid race issues, we sync with
				// the task manager
				synchronized (Scheduler.LockSync) {
					for (Entry<String, Dependency> dependency : dependencies
							.entrySet()) {
						String id = dependency.getKey();
						Resource rsrc = scheduler.getResource(id);
						final Lock lock = rsrc.lock(pid,
								dependency.getValue().type);
						if (lock != null)
							locks.add(lock);
					}
				}

				// And run!
				LOGGER.info("Running task %s", identifier);
				try {
					// Run the task
					state = ResourceState.RUNNING;
					startTimestamp = System.currentTimeMillis();
					int code = doRun(locks);

					if (code != 0)
						throw new RuntimeException(String.format(
								"Error while running the task (code %d)", code));

					// Create the "done" file, update the status and notify
					doneFile.createNewFile();
					state = ResourceState.DONE;
					LOGGER.info("Done");
				} catch (Throwable e) {
					LOGGER.warn("Error while running: %s", e);
					state = ResourceState.ERROR;
				} finally {
					updateDb();
					endTimestamp = System.currentTimeMillis();
					notifyListeners();
				}

				break;
			}
		} catch (UnlockableException e) {
			throw new RuntimeException(e);
		} catch (DatabaseException e) {
			throw new RuntimeException(e);
		} finally {
			// Dispose of all locks
			LOGGER.info("Dispose of locks for %s", this);
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
		// Get the cached status
		Dependency status = dependencies.get(resource.getIdentifier());

		// Is this resource in a state that is good for us?
		int k = resource.accept(status.type).isOK() ? 1 : 0;

		// Computes the difference with the previous status to update the number
		// of unsatisfied resources states
		final int diff = (status.isSatisfied ? 1 : 0) - k;

		LOGGER.info("[%s] Got a notification from %s [%d with %s/%d]", this,
				resource, k, status.type, diff);

		// If the resource has an error / hold state, change our state to
		// "on hold"
		// FIXME: should get an "on hold" counter
		if (resource.getState() == ResourceState.ERROR
				|| resource.getState() == ResourceState.ON_HOLD) {
			if (state != ResourceState.ON_HOLD) {
				state = ResourceState.ON_HOLD;
				scheduler.updateState(this);
			}
		}

		// Update
		nbUnsatisfied += diff;
		if (k == 1)
			status.isSatisfied = true;
		if (diff != 0)
			scheduler.updateState(this);
	}

	// ----- Heap part (do not touch) -----

	private int index;

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	// ----- [/Heap part] -----

	public long getStartTimestamp() {
		return startTimestamp;
	}

	public long getEndTimestamp() {
		return endTimestamp;
	}

	final static DateFormat longDateFormat = DateFormat.getDateTimeInstance();

	@Override
	public void printHTML(PrintWriter out, PrintConfig config) {
		super.printHTML(out, config);
		out.format("<div><b>Lock</b>: %s</div>", isLocked() ? "Locked"
				: "Not locked");
		out.format("<div>%d writer(s) and %d reader(s)</div>", getReaders(),
				getWriters());

		if (getState() == ResourceState.DONE
				|| getState() == ResourceState.ERROR
				|| getState() == ResourceState.RUNNING) {
			long start = getStartTimestamp();
			long end = getState() == ResourceState.RUNNING ? System
					.currentTimeMillis() : getEndTimestamp();

			out.format("<div>Started: %s</div>",
					longDateFormat.format(new Date(start)));
			if (getState() != ResourceState.RUNNING)
				out.format("<div>Ended: %s</div>",
						longDateFormat.format(new Date(end)));
			out.format("<div>Duration: %s</div>",
					Time.formatTimeInMilliseconds(end - start));
		}

		TreeMap<String, Dependency> dependencies = getDependencies();
		if (!dependencies.isEmpty()) {
			out.format("<h2>Dependencies</h2><ul>");
			for (Entry<String, Dependency> entry : dependencies.entrySet()) {
				String dependency = entry.getKey();
				Dependency status = entry.getValue();
				out.format("<li><a href=\"%s/resource?id=%s\">%s</a>: %s</li>",
						config.detailURL, XPMServlet.urlEncode(dependency),
						dependency, status.getType());
			}
			out.println("</ul>");
		}
	}

}
