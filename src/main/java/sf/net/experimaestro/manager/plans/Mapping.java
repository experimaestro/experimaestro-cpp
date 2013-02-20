/*
 * This file is part of experimaestro.
 * Copyright (c) 2013 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.manager.plans;

import sf.net.experimaestro.exceptions.NoSuchParameter;
import sf.net.experimaestro.manager.Task;

import javax.xml.xpath.XPathExpressionException;

/**
 * A mapping from a variable name to a value
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 7/2/13
 */
public abstract class Mapping {
    /**
     * Sets the value
     *
     * @param task
     */
    abstract void set(Task task) throws NoSuchParameter, XPathExpressionException;


    static public interface Set extends Iterable<Mapping> {
    }


}

