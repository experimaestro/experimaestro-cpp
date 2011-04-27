/**
 * 
 */
package sf.net.experimaestro.utils;

import java.lang.reflect.Array;
import java.util.Iterator;

import sf.net.experimaestro.utils.iterators.AbstractIterator;

/**
 * This produces a cartesian product over all the possible combinations
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class CartesianProduct<T> implements Iterable<T[]> {
	private final Iterable<? extends T>[] iterables;
	private final Class<T> klass;
	private final boolean reuse;

	public CartesianProduct(Class<T> klass, Iterable<? extends T>... iterables) {
		this(klass, false, iterables);
	}

	public CartesianProduct(Class<T> klass, boolean reuse,
			Iterable<? extends T>... iterables) {
		this.klass = klass;
		this.reuse = reuse;
		this.iterables = iterables;
	}

	@Override
	public Iterator<T[]> iterator() {
		@SuppressWarnings("unchecked")
		final Iterator<? extends T>[] iterators = new Iterator[iterables.length];

		return new AbstractIterator<T[]>() {
			boolean eof = false;

			@Override
			protected boolean storeNext() {
				if (eof)
					return false;

				if (value == null) {
					// Initialisation
					value = (T[]) Array.newInstance(klass, iterables.length);

					for (int i = 0; i < iterables.length; i++) {
						iterators[i] = iterables[i].iterator();
						if (!iterators[i].hasNext()) {
							eof = true;
							return false;
						}
						value[i] = iterators[i].next();
					}
				} else {
					if (!reuse)
						value = (T[]) Array.newInstance(klass, iterables.length);

					// Next
					for (int i = 0; i < iterables.length; i++) {
						if (!iterators[i].hasNext()) {
							if (iterables.length - 1 == i) {
								eof = true;
								return false;
							}
							iterators[i] = iterables[i].iterator();
							value[i] = iterators[i].next();
						} else {
							// OK - we have found the right iterator
							value[i] = iterators[i].next();
							break;
						}
					}
				}

				return true;
			}
		};
	}

}