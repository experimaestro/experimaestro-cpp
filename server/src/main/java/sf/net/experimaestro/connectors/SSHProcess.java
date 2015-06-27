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

import com.jcraft.jsch.ChannelExec;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.scheduler.Job;
import sf.net.experimaestro.scheduler.TypeIdentifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An SSH process
 */
@TypeIdentifier("ssh")
public class SSHProcess extends XPMProcess {

    transient private ChannelExec channel;

    private SSHProcess() {
    }

    public SSHProcess(SingleHostConnector connector, Job job, ChannelExec channel) {
        super(connector, null, job);
        this.channel = channel;
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
        if (channel != null)
            return super.waitFor();

        while (channel.isConnected()) {
            Thread.sleep(1000);
        }
        return channel.getExitStatus();
    }

    @Override
    public int exitValue() {
        return channel.getExitStatus();
    }

    @Override
    public void destroy() {
        if (channel != null) {
            channel.disconnect();
            channel = null;
        }
    }

    protected void finalize() throws Throwable {
        destroy();
        super.finalize();
    }


    @Override
    public boolean isRunning() {
        return channel.isConnected();
    }
}
