package sf.net.experimaestro.locks;


/**
 * A lock that can be removed
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
	 */
	void changeOwnership(int pid);
}
