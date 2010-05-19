package bpiwowar.expmanager.rsrc;

import java.util.HashSet;
import java.util.Set;

/**
 * The most general type of object manipulated by the server (can be a server, a
 * task, or a data)
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public abstract class Resource implements Comparable<Resource> {
	/**
	 * Task manager
	 */
	transient final TaskManager taskManager;

	/**
	 * Our set of listeners (resources that are listening to changes in the
	 * state of this resource)
	 */
	transient Set<Task> listeners = new HashSet<Task>();

	/**
	 * File based identifier
	 */
	String identifier;

	/**
	 * True when the resource has been generated
	 */
	boolean generated = false;

	/**
	 * Returns a boolean reflecting whether this piece of data has been produced
	 */
	public boolean isReady() {
		return generated;
	}

	abstract public boolean isReady(DependencyType type);
	
	/**
	 * Register a task that waits for our output
	 */
	synchronized public void register(Task task) {
		listeners.add(task);
	}

	/**
	 * Unregister a task
	 */
	public void unregister(Task task) {
		listeners.remove(task);
	}

	/**
	 * Constructs a resource
	 * 
	 * @param taskManager
	 * @param identifier
	 */
	public Resource(TaskManager taskManager, String identifier) {
		this.taskManager = taskManager;
		this.identifier = identifier;
	}

	/**
	 * Called when we have generated the resources (either by running it or
	 * producing it)
	 */
	void notifyListeners(Object... objects) {
		for (Task task : listeners)
			task.notify(this, objects);
	}

	public int compareTo(Resource o) {
		return identifier.compareTo(o.identifier);
	}

	@Override
	public int hashCode() {
		return identifier.hashCode();
	}

	@Override
	public String toString() {
		return identifier.toString();
	}

	
}
