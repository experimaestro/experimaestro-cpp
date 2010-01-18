package bpiwowar.utils;

import java.io.Serializable;

/**
 * @author bpiwowar
 * @date 16/01/2007
 */
public class Pair<T, U> implements Serializable {
	private static final long serialVersionUID = -4235368324324509377L;
	protected T first;
	protected U second;
	
	public static <T,U> Pair<T,U> create(T t, U u) {
		return new Pair<T,U>(t,u);
	}
	
	public Pair() {
	}

	public Pair(final T x, final U y) {
		this.first = x;
		this.second = y;
	}

	public final T getFirst() {
		return first;
	}

	public final void setFirst(final T x) {
		this.first = x;
	}

	public final U getSecond() {
		return second;
	}

	public final void setSecond(final U y) {
		this.second = y;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("(%s,%s)", first, second);
	}
	
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		
		@SuppressWarnings("unchecked")
		Pair<T,U> other = (Pair<T, U>) obj;
		if (first == null) {
			if (other.first != null)
				return false;
		} else if (!first.equals(other.first))
			return false;
		if (second == null) {
			if (other.second != null)
				return false;
		} else if (!second.equals(other.second))
			return false;
		return true;
	}


}
