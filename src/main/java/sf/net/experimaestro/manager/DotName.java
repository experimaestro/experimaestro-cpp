package sf.net.experimaestro.manager;

import java.util.Arrays;

import bpiwowar.argparser.utils.Output;

/**
 * A variable name with various levels separated by dots
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class DotName implements Comparable<DotName> {
	/**
	 * The qualified name
	 */
	private String[] array;

	/**
	 * Construction
	 */
	public DotName(String[] array) {
		super();
		this.array = array;
	}

	/**
	 * Creates a qualified name which is a prefix of a given qualified name
	 * 
	 * @param key
	 *            The qualified name
	 * @param length
	 *            The
	 */
	public DotName(DotName key, int length) {
		array = new String[length];
		for (int i = 0; i < length; i++)
			array[i] = key.array[i];
	}

	/**
	 * Creates a qualified name with a new prefix
	 * @param prefix The prefix
	 * @param qName The qualified name that is used as a base
	 */
	public DotName(String prefix, DotName qName) {
		this.array = new String[1 + qName.size()];
		this.array[0] = prefix;
		for (int i = 0; i < qName.size(); i++)
			this.array[i + 1] = qName.array[i];
	}

	/**
	 * Creates an unqualified name
	 */
	public DotName(String name) {
		this.array = new String[] { name };
	}

	/**
	 * Returns a new qualified name with offset
	 * 
	 * @param offset
	 * @return
	 */
	public DotName offset(int offset) {
		String[] name = new String[array.length - offset];
		for (int i = offset; i < array.length; i++)
			name[i - offset] = array[i];
		return new DotName(name);
	}

	@Override
	public String toString() {
		return Output.toString(".", array);
	}

	public int size() {
		return array.length;
	}

	/**
	 * Returns the length of the common prefix
	 */
	public int commonPrefixLength(DotName o) {
		if (o == null)
			return 0;

		int min = Math.min(array.length, o.array.length);
		for (int i = 0; i < min; i++) {
			int z = array[i].compareTo(o.array[i]);
			if (z != 0)
				return i;
		}
		return min;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(array);
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DotName other = (DotName) obj;
		if (!Arrays.equals(array, other.array)) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(DotName o) {
		// Compare package name first
		int min = Math.min(array.length, o.array.length);
		for (int i = 0; i < min; i++) {
			int z = array[i].compareTo(o.array[i]);
			if (z != 0)
				return z;
		}

		// The longer one then?
		int z = array.length - o.array.length;
		return z;
	}

	/**
	 * Return the unqualified name
	 */
	public String getName() {
		return array[array.length - 1];
	}

	/**
	 * Return the index<sup>th</sup> element of the qualified name
	 */
	public String get(int index) {
		return array[index];
	}

	/**
	 * Creates a DotName from an unparsed string identifier containing
	 * dots.
	 * 
	 * @param name The full identifier to be parsed
	 * @return
	 */
	public static DotName parse(String name) {
		return new DotName(name.split("\\."));
	}

}
