package bpiwowar.expmanager.locks;

/**
 * Possible lock types on a resource
 * 
 * <p>
 * {@link #READ_ACCESS} and {@link #GENERATED} imply that the resource should be
 * generated before use
 * </p>
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public enum LockType {
	/**
	 * Asks for a read access
	 */
	READ_ACCESS,

	/**
	 * Waits for a read/write access
	 */
	WRITE_ACCESS,

	/**
	 * Waits for an exclusive access
	 */
	EXCLUSIVE_ACCESS,

	/**
	 * Just asks that the data be generated
	 */
	GENERATED,
}
