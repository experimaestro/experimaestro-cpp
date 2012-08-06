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

package sf.net.experimaestro.scheduler;

import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.utils.log.Logger;

/**
 * 
 * A single piece of data that can be locked in various ways
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class SimpleData extends Data {
	final static private Logger LOGGER = Logger.getLogger();

	public SimpleData(Scheduler taskManager, Connector connector, String identifier, LockMode mode,
			boolean generated) {
		super(taskManager, connector, identifier, mode);
		LOGGER.info(
				"New resource: simple data (%s) with mode %s (generated = %b)",
				identifier, mode, generated);
		this.state = generated ? ResourceState.DONE : ResourceState.WAITING;
	}

    public SimpleData(Scheduler scheduler, Locator identifier, LockMode lockMode, boolean generated) {
        super(scheduler, identifier, lockMode, generated);
        this.state = generated ? ResourceState.DONE : ResourceState.WAITING;
    }
}
