/*
 *
 *  This file is part of experimaestro.
 *  Copyright (c) 2011 B. Piwowarski <benjamin@bpiwowar.net>
 *
 *  experimaestro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  experimaestro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package sf.net.experimaestro.utils;

import sf.net.experimaestro.utils.log.Logger;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;


/**
 * Helper methods for generics
 * 
 * @author bpiwowar
 */
public class GenericHelper {
	final static private Logger logger = Logger.getLogger();

	final public static <T> ArrayList<T> newArrayList() {
		return new ArrayList<T>();
	}

	final public static <K, V> TreeMap<K, V> newTreeMap() {
		return new TreeMap<K, V>();
	}

	final public static <K, V> HashMap<K, V> newHashMap() {
		return new HashMap<K, V>();
	}

	final public static <T> HashSet<T> newHashSet() {
		return new HashSet<T>();
	}

	/**
	 * Reflection helper - give the generic parameters of a class.
	 * 
	 * @param candidate
	 *            The class implementing the superclass or superinterface
	 * @param implemented
	 * @return a type array of the type arguments of a superclass or a
	 *         superinterface
	 */
	public static Type[] getActualTypeArguments(Class<?> candidate,
			Class<?> implemented) {
		// We are not a derived class ?
		if (!implemented.isAssignableFrom(candidate)) {
			return null;
		}

		// Get the list of candidates
		final boolean isInterface = implemented.isInterface();
		Class<?> current = candidate;

		do {
			final Type[] list = isInterface ? current.getGenericInterfaces()
					: new Type[] { current.getGenericSuperclass() };
			for (Type anInterface : list) {
				if (anInterface instanceof ParameterizedType) {
					ParameterizedType ptype = (ParameterizedType) anInterface;
					if (ptype.getRawType() == implemented) {
						return ptype.getActualTypeArguments();
					}
				}
			}

			current = current.getSuperclass();
		} while (!isInterface && current != null);

		// Should not happen!
		throw new RuntimeException(
				String
						.format(
								"Should not happen: we did not find the generic type of %s in %s",
								implemented, current));
	}

	public static <T> TreeSet<T> newTreeSet() {
		return new TreeSet<T>();
	}
}
