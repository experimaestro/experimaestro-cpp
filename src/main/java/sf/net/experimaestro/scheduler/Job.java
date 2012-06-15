/*
 *
 *  This file is part of experimaestro.
 *  Copyright (c) 2011 B. Piwowarski <benjamin@bpiwowar.net>
 *
 *  experimaestro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  experimaestro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package sf.net.experimaestro.scheduler;

import static java.lang.String.format;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map.Entry;
import java.util.TreeMap;

import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.locks.LockType;
import sf.net.experimaestro.locks.UnlockableException;
import sf.net.experimaestro.server.XPMServlet;
import sf.net.experimaestro.utils.HeapElement;
import sf.net.experimaestro.utils.ProcessUtils;
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
public abstract class Job extends Resource implements HeapElement<Job>, Runnable {
	final static private Logger LOGGER = Logger.getLogger();
    static final String DONE_EXTENSION = ".done";


    protected Job() {
	}

	/**
	 * Initialisation of a task
	 * 
	 * @param scheduler The job scheduler
	 */
	public Job(Scheduler scheduler, Connector connector, String identifier) {
		super(scheduler, connector, identifier, LockMode.EXCLUSIVE_WRITER);
		state = isDone() ? ResourceState.DONE : ResourceState.WAITING;
	}

	private boolean isDone() {
        try {
            return getConnector().fileExists(identifier.path + ".done");
        } catch (Exception e) {
            LOGGER.error("Error while checking if " + identifier + ".done exists");
            return false;
        }
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
	 * When did the job start (0 if not started)
	 */
	private long startTimestamp;

	/**
	 * When did the job stop (0 when it did not stop yet)
	 */
	long endTimestamp;

    /**
     * Our job monitor
     */
    JobMonitor jobMonitor;

	@Override
	protected boolean isActive() {
		return super.isActive() || state == ResourceState.WAITING
				|| state == ResourceState.RUNNING;
	}

	/**
	 * The dependencies for this job (dependencies are on any resource)
	 */
	private TreeMap<Locator, Dependency> dependencies = new TreeMap<Locator, Dependency>();

	/**
	 * Number of unsatisfied dependencies
	 */
	int nbUnsatisfied;

	/**
	 * The set of dependencies for this object
	 * 
	 * @return
	 */
	public TreeMap<Locator, Dependency> getDependencies() {
		return dependencies;
	}

	/**
	 * Add a dependency to another resource
	 *
     * @param resource The resource to lock
	 * @param type The type of lock that is asked
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
			dependencies.put(new Locator(resource.identifier),
					new Dependency(type, ready));
		}
	}

	@Override
	protected void finalize() throws Throwable {
		for (Locator id : dependencies.keySet()) {
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
	 *
     * @param locks
     *            The set of locks that were taken
     *
     * @return The error code (0 if everything went fine)
	 * @throws Throwable
	 */
	abstract protected JobMonitor startJob(ArrayList<Lock> locks) throws Throwable;

    @Override
    public void init(Scheduler scheduler) {
        super.init(scheduler);
        if (jobMonitor != null)
            jobMonitor.setJob(this);
    }

    /*
      * (non-Javadoc)
      *
      * @see java.lang.Runnable#run()
      */
	final public void run() {
		// Check if the task has already been done
        try {
            if (getConnector().fileExists(identifier.path + DONE_EXTENSION)) {
                LOGGER.info("Task %s is already done", identifier);
                return;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        // Our locks
		ArrayList<Lock> locks = new ArrayList<Lock>();

		try {
			while (true) {
				// Check if not done
				if (getConnector().fileExists(identifier.path + DONE_EXTENSION)) {
					LOGGER.info("Task %s is already done", identifier);
					return;
				}

				// Try to lock, otherwise wait
				try {
					locks.add(getConnector().createLockFile(identifier.path + LOCK_EXTENSION));
				} catch (UnlockableException e) {
					LOGGER.info("Could not lock job [%s]", identifier);
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
				if (getConnector().fileExists(identifier.path + DONE_EXTENSION)) {
					LOGGER.info("Task %s is already done", identifier);
					return;
				}

				String pid = String.valueOf(ProcessUtils.getPID());

				// Now, tries to lock all the resources
				// in order to avoid race issues, we sync with
				// the task manager
				synchronized (Scheduler.LockSync) {
					for (Entry<Locator, Dependency> dependency : dependencies
							.entrySet()) {
						Locator id = dependency.getKey();
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
					// Change the state
					state = ResourceState.RUNNING;
					startTimestamp = System.currentTimeMillis();
					updateDb();

                    // Start the task and transfer locking handling to those
                    JobMonitor monitor = startJob(locks);
                    locks.clear();
                    int code = monitor.waitFor();

					if (code != 0)
						throw new RuntimeException(String.format(
								"Error while running the task (code %d)", code));

					// Create the "done" file, update the status and notify
                    getConnector().touchFile(identifier.path + ".done");
					state = ResourceState.DONE;
					LOGGER.info("Done");
				} catch (Throwable e) {
					LOGGER.warn(format("Error while running: %s", this), e);
					state = ResourceState.ERROR;
				} finally {
                    endTimestamp = System.currentTimeMillis();
                    updateDb();
					notifyListeners();
				}

				break;
			}
		} catch (UnlockableException e) {
			throw new RuntimeException(e);
		} catch (DatabaseException e) {
			throw new RuntimeException(e);
		} catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
			// Dispose of all locks
			LOGGER.info("Disposing of locks for %s", this);
			for (Lock lock : locks)
				lock.dispose();
		}

	}

	/**
	 * Called when a resource status has changed
	 * 
	 * @param resource
	 *            The resource has changed (or null if itself)
	 * @param objects
	 *            Optional parameters
	 */
	synchronized public void notify(Resource resource, Object... objects) {
		// Self-notification: discard
		if (resource == null)
			return;

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

		TreeMap<Locator, Dependency> dependencies = getDependencies();
		if (!dependencies.isEmpty()) {
			out.format("<h2>Dependencies</h2><ul>");
			out.format("<div>%d unsatisfied dependencie(s)</div>",
					nbUnsatisfied);
			for (Entry<Locator, Dependency> entry : dependencies.entrySet()) {
				Locator dependency = entry.getKey();
				Dependency status = entry.getValue();
				Resource resource = null;
				try {
					resource = scheduler.getResource(entry.getKey());
				} catch (DatabaseException e) {
				}
				out.format(
						"<li><a href=\"%s/resource?id=%s\">%s</a>: %s [%b]</li>",
						config.detailURL, XPMServlet.urlEncode(dependency.toString()),
						dependency, status.getType(), resource == null ? false
								: resource.accept(status.type).isOK());
			}
			out.println("</ul>");
		}
	}

}
