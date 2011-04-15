package sf.net.experimaestro.utils;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Heaps are binary tree for which a node value is smaller than the value of any
 * of its children. Two nodes can have the same value.
 * 
 * arrays for which a[k] <= a[2*k+1] and a[k] <= a[2*k+2] for all k
 * 
 * For a node k,
 * <ul>
 * <li>(k-1) / 2 is the parent</li>
 * <li>2 * k + 1 is the left child</li>
 * <li>2 * k + 2 is the right child</li>
 * </ul>
 * 
 * @author bpiwowar
 * 
 */
final public class Heap<E extends HeapElement<E>> implements Iterable<E> {

	/**
	 * Default element for a heap
	 * 
	 * @author bpiwowar
	 * @date Nov 20, 2007
	 * 
	 * @param <E>
	 */
	static abstract public class DefaultElement<E> implements HeapElement<E> {
		int index = 0;

		/*
		 * (non-Javadoc)
		 * 
		 * @see yrla.utils.HeapElement#getIndex()
		 */
		public int getIndex() {
			return index;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see yrla.utils.HeapElement#setIndex(int)
		 */
		public void setIndex(int index) {
			this.index = index;

		}
	}

	List<E> list = new ArrayList<E>();

	Comparator<E> comparator = null;

	/**
	 * Default construction
	 */
	@SuppressWarnings("unchecked")
	public Heap() {
		// We use the default comparator, but this might generate exceptions
		// latter
		this.comparator = new DefaultComparator();
	}

	/**
	 * Default construction
	 */
	public Heap(Comparator<E> comparator) {
		this.comparator = comparator;
	}

	/**
	 * Construct with a list
	 */
	public Heap(List<E> list) {
		if (!verify())
			throw new IllegalArgumentException(
					"The list given for the heap does not verify the heap property");
	}

	/**
	 * Swap two list elements, taking care of updating the indices
	 * 
	 * @param i
	 *            The index of the first element
	 * @param j
	 *            The index of the second element
	 */
	private void swap(final int i, final int j) {
		final E x = list.get(i);
		final E y = list.get(j);

		// Swap indices
		final int k = y.getIndex();
		y.setIndex(x.getIndex());
		x.setIndex(k);

		list.set(i, y);
		list.set(j, x);
	}

	/**
	 * Add a new item to the heap
	 * 
	 * @param item
	 *            The item to add
	 */
	public void add(final E item) {
		list.add(item);
		int i = list.size() - 1;
		list.get(i).setIndex(i);
		while (i > 0) {
			if (comparator.compare(list.get((i - 1) / 2), list.get(i)) <= 0)
				break;
			swap((i - 1) / 2, i);
			i = (i - 1) / 2;
		}
	}

	private void setItem(final int index, final E item) {
		list.set(index, item);
		item.setIndex(index);
	}

	/**
	 * Return the smallest element, and update the heap structure
	 * 
	 * @return
	 */
	public E pop() {
		final E return_value = list.get(0);
		int hole_index = 0;
		int new_hole_index = 0;
		final int L = size();

		while (true) {
			final int l = 2 * hole_index + 1;
			if (l >= L)
				break;

			if (l + 1 >= L) {
				setItem(hole_index, list.get(l));
				hole_index = l;
				break;
			} else {
				if (comparator.compare(list.get(2 * hole_index + 1), list
						.get(2 * hole_index + 2)) < 0)
					new_hole_index = l;
				else
					new_hole_index = l + 1;

				if (comparator.compare(list.get(L - 1), list
						.get(new_hole_index)) > 0) {
					setItem(hole_index, list.get(new_hole_index));
					hole_index = new_hole_index;
				} else
					break;
			}
		}

		if (hole_index < L - 1)
			setItem(hole_index, list.get(L - 1));
		list.remove(L - 1);
		return return_value;
	}

	private int compare(int j, int k) {
		return comparator.compare(list.get(j), list.get(k));
	}

	/**
	 * Update the value of a heap member
	 * 
	 * @param item
	 */
	public void update(final E item) {
		int k = item.getIndex();
		final int size = size();
		if (k > 0 && compare(k, (k - 1) / 2) < 0) {
			// Case where we go up (parent > item)
			// We swap with the parent until everything we are verifying the
			// invariant
			int p = (k - 1) / 2;
			do {
				swap(k, p);
				k = p;
				p = (k - 1) / 2;
			} while (k > 0 && compare(k, p) < 0);

		} else
			// Case where we go down (parent < item)
			while (true) {
				int mn;
				final int left = k * 2 + 1; // left child
				if (left >= size)
					break;

				// Choose the left child if (1) there is no right child
				// or (2) the left child is smaller than the right one
				if (left + 1 >= size || compare(left, left + 1) < 0)
					mn = left;
				else
					mn = left + 1;

				// item <= min(child): OK
				if (compare(k, mn) <= 0)
					break;
				// otherwise, swap item with min
				swap(k, mn);
				k = mn;
			}
	}

	public int size() {
		return list.size();
	}

	public E peek() {
		return list.get(0);
	}

	/**
	 * Verify the tree integrity (useful for test and serialization)
	 */
	private boolean verify(final int k) {
		if (k < size()) {
			if (list.get(k).getIndex() != k)
				return false;
			if (k > 0) {
				final int p = (k - 1) / 2;
				if (comparator.compare(list.get(p), list.get(k)) > 0)
					return false;
			}
			return verify(2 * k + 1) && verify(2 * k + 2);
		}

		return true;
	}

	/**
	 * Verify the integrity of the heap
	 * 
	 * @return True if the tree is all right
	 */
	public boolean verify() {
		return verify(0);
	}

	public void printTree(final PrintStream out) {
		printTree(out, 0);
	}

	private void printTree(final PrintStream out, final int k) {
		if (k >= list.size())
			out.print("ø");
		else if (2 * k + 1 >= size())
			out.format("%s/%d", list.get(k), list.get(k).getIndex());
		else {
			out.format("%s/%d (", list.get(k), list.get(k).getIndex());
			printTree(out, 2 * k + 1);
			out.print(",");
			printTree(out, 2 * k + 2);
			out.print(")");
		}

		if (k == 0)
			out.println();
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}

	public Iterator<E> iterator() {
		return list.iterator();
	}

}