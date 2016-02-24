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

import com.jcraft.jsch.ChannelExec;
import net.bpiwowar.xpm.exceptions.ConnectorException;
import net.bpiwowar.xpm.exceptions.LaunchException;
import net.bpiwowar.xpm.exceptions.WrappedException;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.scheduler.Job;
import net.bpiwowar.xpm.scheduler.TypeIdentifier;
import net.bpiwowar.xpm.utils.log.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An SSH process
 */
@TypeIdentifier("ssh")
public class SSHProcess extends XPMProcess {
    final static private Logger LOGGER = Logger.getLogger();
    transient private ChannelExec channel;
    boolean detached = true;

    private SSHProcess() {
    }

    public SSHProcess(SingleHostConnector connector, Job job, ChannelExec channel, boolean detached) {
        super(connector, null, job);
        this.channel = channel;
        this.detached = detached;
        startWaitProcess();
    }

    @Override
    public OutputStream getOutputStream() {
        try {
            return channel.getOutputStream();
        } catch (IOException e) {
            throw new XPMRuntimeException(e);
        }
    }

    @Override
    public InputStream getInputStream() {
        try {
            return channel.getInputStream();
        } catch (IOException e) {
            throw new XPMRuntimeException(e);
        }
    }

    @Override
    public InputStream getErrorStream() {
        try {
            return channel.getErrStream();
        } catch (IOException e) {
            throw new XPMRuntimeException(e);
        }
    }

    @Override
    public int waitFor() throws InterruptedException {
        // We don't have any open channel
        if (channel == null)
            return super.waitFor();

        // Use channel
        while (channel.isConnected()) {
            Thread.sleep(1000);
        }

        // Detached mode: don't use the exit status (the channel might be disconnect for various reasons)
        if (detached) return super.waitFor();
        return channel.getExitStatus();
    }

    @Override
    public int exitValue(boolean checkFile) {
        if (channel != null) {
            if (channel.isConnected()) {
                throw new IllegalThreadStateException("Process has not ended");
            }
            if (!detached) {
                return channel.getExitStatus();
            }
        }
        return super.exitValue(checkFile);
    }

    @Override
    public void destroy() {
        // Detached: we have to kill the job on the host
        if (detached) {
            try {
                if (isRunning(true)) {
                    // First check that the job is running
                    final AbstractProcessBuilder killCommand = getConnector().processBuilder().command("kill", pid);
                    killCommand.execute(LOGGER);
                }
            } catch (LaunchException | ConnectorException | InterruptedException | IOException e) {
                throw new WrappedException(e);
            }
        }

        // Not detached: we just disconnect
        if (channel != null) {
            channel.disconnect();
            channel = null;
        } else throw new RuntimeException("Could not destroy process: SSH channel is not defined");
    }

    @Override
    public boolean isRunning(boolean checkFiles) throws ConnectorException {
        if (channel != null) {
            // If the channel is not connected, we check if in detached mode using files
            return channel.isConnected() || (detached && super.isRunning(checkFiles));
        }
        return super.isRunning(checkFiles);
    }
}
