package sf.net.experimaestro.scheduler;

import sf.net.experimaestro.locks.LockType;

import com.sleepycat.persist.model.Persistent;

/**
 * What is the status of a dependency This class stores the previous status
 * (satisfied or not) in order to update the number of blocking resources
 */
@Persistent
public class Dependency {
	/**
	 * Type of lock that we request on the dependency 
	 */
	LockType type = null;
	
	/**
	 * Was this dependency satisfied when we last checked?
	 */
	boolean isSatisfied = false;

	protected Dependency() {
	}

	public Dependency(LockType type, boolean isSatisfied) {
		this.type = type;
		this.isSatisfied = isSatisfied;
	}

	public LockType getType() {
		return type;
	}

}