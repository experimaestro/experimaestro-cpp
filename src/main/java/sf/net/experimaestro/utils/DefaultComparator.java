package sf.net.experimaestro.utils;

import java.util.Comparator;

/**
 * A default comparator (using the default compareTo method of the object)
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class DefaultComparator<T extends Comparable<T>> implements Comparator<T> {
	public int compare(T o1, T o2) {
		return o1.compareTo(o2);
	}
}
