package sf.net.experimaestro.utils.arrays;

import java.lang.reflect.Array;
import java.util.AbstractList;

public class ListAdaptator<T> extends AbstractList<T> {
	private Object array;
	private int size;
	private int from;
	
	/**
	 * Construct a list adaptator from an array
	 * @param array
	 */
	public ListAdaptator(Object array) {
		this.array = array;
		this.from = 0;
		this.size = Array.getLength(array);;
	}

	public ListAdaptator(Object array, int from, int size) {
		this.array = array;
		this.from = from;
		this.size = size;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public T get(int index) {
		return (T) Array.get(array, index + from);
	}

	@Override
	public int size() {
		return size;
	}
	
	@SuppressWarnings("unchecked")
	public T set(int index, T value) {
		T t = (T) Array.get(array, index);
		Array.set(array, index, value);
		return t;
	};

	public static ListAdaptator<Double> get(double[] array) {
		return new ListAdaptator<Double>(array);
	}
	public static ListAdaptator<Integer> get(int[] array) {
		return new ListAdaptator<Integer>(array);
	}
	public static ListAdaptator<Long> get(long[] array) {
		return new ListAdaptator<Long>(array);
	}

	public static <T> ListAdaptator<T> get(T[] split) {
		return new ListAdaptator<T>(split);
	}

	public static <T> ListAdaptator<T> create(T [] array) {
		return new ListAdaptator<T>(array);
	}

}
