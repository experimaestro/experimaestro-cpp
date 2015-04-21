package sf.net.experimaestro.connectors;

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

import sf.net.experimaestro.scheduler.Job;
import sf.net.experimaestro.utils.arrays.ListAdaptator;

import java.nio.file.FileSystemException;
import java.util.List;

/**
 * This class is used to createSSHAgentIdentityRepository processes on a given host through a
 * specific connection method (e.g. direct, ssh).
 * <p/>
 * This class borrows heavily from {@linkplain java.lang.ProcessBuilder},
 * with some differences:
 * <ol>
 * <li>There is a parameter to detach the process (e.g. for SSH connections)</li>
 * <li>A {@linkplain Job} can be associated to the process for notification</li>
 * <li>A path should be associated (if detached) to the process</li>
 * </ol>
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
abstract public class AbstractProcessBuilder extends AbstractCommandBuilder {

    /**
     * The command to run
     */
    private List<String> command;

    public AbstractProcessBuilder command(List<String> command) throws FileSystemException {
        this.command = command;
        return this;
    }

    final public AbstractProcessBuilder command(String... command) throws FileSystemException {
        return command(ListAdaptator.create(command));
    }


    public List<String> command() {
        return command;
    }


}
