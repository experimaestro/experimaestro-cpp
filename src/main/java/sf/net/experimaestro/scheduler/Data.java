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

import com.sleepycat.persist.model.Entity;
import sf.net.experimaestro.connectors.Connector;

/**
 * Represents some data that can be produced by a given job
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Entity
public abstract class Data extends Resource {

	public Data(Scheduler taskManager, Connector connector, String path, LockMode mode) {
		super(taskManager, connector, path, mode);
	}

	/**
	 * The job that can or has generated this data (if any)
	 */
	transient Job generatingJob = null;

    public Data(Scheduler scheduler, ResourceLocator identifier, LockMode lockMode, boolean exists) {
        super(scheduler, identifier, lockMode);

    }
}
