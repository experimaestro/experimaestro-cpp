/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
 *
 * experimaestro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * experimaestro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 */

package sf.net.experimaestro.scheduler;

import bpiwowar.argparser.EnumValue;

/**
 * Locking mode.
 *
 * Defines how a resource can be accessed. For the moment,
 * <ul>
 *     <li>Read-only</li>
 *     <li>Exclusive writer</li>
 *     <li>Single writer with concurrent multiple reader</li>
 *     <li>Multiple writer</li>
 * </ul>
 */
public enum LockMode {
	@EnumValue(value = "read-only", help = "Creates a resource that can only be read")
	/**
	 * The resource need not (and cannot) be locked
	 */
	READ_ONLY,

	/**
	 * The resouce is locked globally (e.g. a generated file)
	 */
	@EnumValue(value = "exclusive-writer", help = "Creates a resource that can be read by many, but need exclusive write access")
	EXCLUSIVE_WRITER,

	/**
	 * A data resource that can be read by many, but written by at most one
	 * (e.g. Berkeley database with concurrent read but just one write access)
	 */
	@EnumValue(value = "single-writer", help = "Creates a resource that can be read by many, and can be written by at most one")
	SINGLE_WRITER,

	/**
	 * A resouce that can be read/write by many (typically a server). In this
	 * mode, no lock mechanism is used
	 */
	@EnumValue(value = "multiple-writer", help = "Creates a resource that can be read and written by many")
	MULTIPLE_WRITER,


    /**
     * Custom mode: the locking mechanism is managed by the resource itself
     */
    CUSTOM

}