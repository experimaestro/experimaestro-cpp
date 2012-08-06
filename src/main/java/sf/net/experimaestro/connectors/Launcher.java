/*
 *
 *  This file is part of experimaestro.
 *  Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.connectors;

import com.sleepycat.persist.model.Persistent;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.scheduler.CommandLineTask;

import java.util.ArrayList;

/**
 * Defines how to run a command line and to monitor changes in
 * execution state.
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Persistent
public abstract class Launcher {
    /** Connector */
    private final Connector connector;

    public Launcher(Connector connector) {
        this.connector = connector;
        if (!connector.canExecute())
            throw new IllegalArgumentException(String.format("Connector %s cannot be used to execute commands", connector.getIdentifier()));
    }

    /** Launch the task */
    public abstract JobMonitor launch(CommandLineTask task, ArrayList<Lock> locks) throws Exception;


    static public Launcher create(String identifier, Connector connector) {
        switch (identifier) {
            case "sh":
                return new ShLauncher(connector);
            case "oar":
                return new OARLauncher(connector);
            default:
                throw new IllegalArgumentException(String.format("Unknown launcher type: %s", identifier));
        }
    }

}
