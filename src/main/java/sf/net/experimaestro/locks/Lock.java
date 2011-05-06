package sf.net.experimaestro.locks;

/**
 * A lock that can be removed.
 * 
 * The lock is taken during the object construction which is dependent on the
 * actual {@link Lock} implementation.
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public interface Lock {
	/**
	 * Dispose of the resource - returns true if the resource was properly
	 * unlocked
	 */
	boolean dispose();

	/**
	 * Change ownership
	 * 
	 * @param pid
	 *            The new owner PID
	 */
	void changeOwnership(int pid);
}
