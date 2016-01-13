package net.bpiwowar.xpm.manager;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
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

import net.bpiwowar.xpm.exceptions.NoSuchParameter;
import net.bpiwowar.xpm.manager.json.Json;
import net.bpiwowar.xpm.utils.JsonAbstract;

/**
 * Defines a connection to between one or more output values and one input
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@JsonAbstract
public abstract class Connection {
    /**
     * The destination
     */
    final DotName to;

    /**
     * Required flag
     */
    boolean required;

    public Connection(DotName to) {
        this.to = to;
    }

    /**
     * Get the list of the input variables
     *
     * @return
     */
    abstract public Iterable<String> inputs();

    /**
     * Compute the value
     *
     * @param task
     * @return
     */
    public abstract Json computeValue(Task task) throws NoSuchParameter;
}
