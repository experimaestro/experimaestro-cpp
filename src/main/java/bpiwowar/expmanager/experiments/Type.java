package bpiwowar.expmanager.experiments;

import java.util.Set;
import java.util.TreeSet;

/**
 * A value type
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Type implements Comparable<Type> {
	final public static Type INTEGER = new Type("java.lang.Integer"),
			DOUBLE = new Type("java.lang.Double");

	/**
	 * Supertypes (each type can be linked to other types)
	 */
	Set<Type> supertypes = new TreeSet<Type>();

	/**
	 * Identifier for this type
	 */
	String id;

	/**
	 * The view for this type (for example, it can be a given type can be either
	 * given as a String or as a File that contains the string)
	 */
	String view;

	/**
	 * Version of the type
	 */
	int version = 1;

	/**
	 * Format version
	 */
	int formatVersion = 1;

	/**
	 * Creates a new type
	 */
	public Type(String id) {
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Type o) {
		return this.id.compareTo(o.id);
	}

}
