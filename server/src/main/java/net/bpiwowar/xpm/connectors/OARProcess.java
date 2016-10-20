package net.bpiwowar.xpm.connectors;

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

import net.bpiwowar.xpm.exceptions.ConnectorException;
import net.bpiwowar.xpm.exceptions.WrappedException;
import net.bpiwowar.xpm.scheduler.Job;
import net.bpiwowar.xpm.scheduler.TypeIdentifier;
import net.bpiwowar.xpm.utils.log.Logger;
import org.apache.commons.lang.NotImplementedException;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * An OAR process
 */
@TypeIdentifier("oar")
public class OARProcess extends XPMProcess {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * Used for serialization
     */
    public OARProcess() {
    }

    public OARProcess(Job job, String pid, SingleHostConnector connector) {
        super(connector, pid, job);
        startWaitProcess();
    }


    @Override
    public OutputStream getOutputStream() {
        throw new NotImplementedException();
    }

    @Override
    public InputStream getInputStream() {
        throw new NotImplementedException();
    }

    @Override
    public InputStream getErrorStream() {
        throw new NotImplementedException();
    }

    @Override
    public boolean isRunning() throws ConnectorException {
        OARStat oarStat = new OARStat(getConnector(), pid, true);
        return oarStat.isRunning();
    }

    @Override
    public void destroy() {
        try {
            // First check that we are running
            final AbstractProcessBuilder builder = getConnector().processBuilder();
            builder.command("oardel", "--signal", "TERM", pid);
            builder.detach(false);
            builder.execute(LOGGER);
        } catch (Exception e) {
            throw new WrappedException(e);
        }
    }

}
