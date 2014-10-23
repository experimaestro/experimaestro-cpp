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

package sf.net.experimaestro.manager;

import java.nio.file.Path;

/**
 * A repository for tasks, types and modules
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
abstract public class AbstractRepository {

    /**
     * Our repository identifier (i.e. the script identifier)
     */
    Path identifier;

    public AbstractRepository(Path identifier) {
        this.identifier = identifier;
    }

    /**
     * Get a task factory given a qualified name
     */
    public abstract TaskFactory getFactory(QName name);

    /**
     * Get a type definition given a qualified name
     */
    public abstract Type getType(QName name);
}
