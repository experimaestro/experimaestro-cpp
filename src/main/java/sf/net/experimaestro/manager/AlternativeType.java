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

package sf.net.experimaestro.manager;

import java.util.HashMap;
import java.util.Map;

public class AlternativeType extends Type {
	private static final long serialVersionUID = 1L;

	/**
	 * The task factories that handles the values
	 */
	Map<QName, TaskFactory> factories = new HashMap<QName, TaskFactory>();

	/**
	 * Create a new type with alternatives
	 * @param qName
	 */
	public AlternativeType(QName qName) {
		super(qName);
	}

	/**
	 * Add a new factory
	 * @param name
	 * @param factory
	 */
	public void add(QName name, TaskFactory factory) {
		factories.put(name, factory);
	}


}
