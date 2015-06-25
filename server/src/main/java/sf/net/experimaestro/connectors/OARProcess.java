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

import org.apache.commons.lang.NotImplementedException;
import org.w3c.dom.Document;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.scheduler.Job;
import sf.net.experimaestro.scheduler.TypeIdentifier;
import sf.net.experimaestro.utils.log.Logger;

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
    public void destroy() {
        throw new NotImplementedException();
    }

//
//    @Override
//    public boolean isRunning() {
//        final Document document = oarstat(false);
//        String state = OARLauncher.evaluateXPathToString("//item[@key = \"state\"]/text()", document);
//        LOGGER.debug("State of OAR process %s is %s", pid, state);
//        return "running".equalsIgnoreCase(state);
//    }
//
//    @Override
//    public int exitValue() {
//        final Document document = oarstat(true);
//        String state = OARLauncher.evaluateXPathToString("//item[@key = \"state\"]/text()", document);
//        if ("running".equalsIgnoreCase(state))
//            throw new IllegalThreadStateException("Job is running - cannot access its exit value");
//
//        String code = OARLauncher.evaluateXPathToString("//item[@key = \"exit_code\"]/text()", document);
//
//        LOGGER.debug("Exit code of OAR process %s is %s", pid, code);
//
//        if ("".equals(code))
//            return -1;
//        return Integer.parseInt(code);
//    }

    /**
     * Runs oarstat and returns the XML document
     */
    private Document oarstat(boolean full) {
        try {
            if (full) {
                return OARLauncher.exec(getConnector(), "oarstat", "--xml",  "--full", "--job", pid);
            }
            return OARLauncher.exec(getConnector(), "oarstat", "--xml", "--job", pid);
        } catch (Exception e) {
            throw new XPMRuntimeException(e, "Cannot parse oarstat output");
        }
    }

}
