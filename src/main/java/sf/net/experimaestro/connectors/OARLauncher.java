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
import org.w3c.dom.Document;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.exceptions.LaunchException;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.scheduler.CommandLineTask;
import sf.net.experimaestro.scheduler.Job;
import sf.net.experimaestro.utils.log.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.ArrayList;

/**
 * A command line launcher with OAR
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Persistent
public class OARLauncher extends UnixShellLauncher {
    static private final Logger LOGGER = Logger.getLogger();

    // Command to start
    private Object oarCommand = "oarsub";

    // Prefix for the PID of the job
    private final String OARJOBID_PREFIX = "OAR_JOB_ID=";

    /** Construction from a connector */
    public OARLauncher(SingleHostConnector connector) {
        super(connector);
    }

    /**
     * Helper method that executes a command that produces XML, and returns a DOM document from it
     */
    private static Document exec(Job job, ArrayList<Lock> locks, String command) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        final XPMProcess process = job.getConnector().exec(job, command, locks, false, null, null);
        return dBuilder.parse(process.getInputStream());
    }

    /** Evaluate an XPath to a string */
    static private String evaluateXPathToString(String expression, Document document) {
        String value;
        XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            value = (String) xpath.evaluate(expression, document,
                    XPathConstants.STRING);
        } catch (XPathExpressionException e) {
            throw new ExperimaestroRuntimeException(e, "Cannot evaluted XPath expression [%s]", xpath);
        }
        return value;
    }

    @Override
    public XPMProcessBuilder processBuilder(SingleHostConnector connector) {
        return new ProcessBuilder(connector);
    }


    public class ProcessBuilder extends XPMProcessBuilder {

        private SingleHostConnector connector;

        public ProcessBuilder(SingleHostConnector connector) {
            this.connector = connector;
        }

        @Override
        public XPMProcess start() throws LaunchException, IOException {

            final String path = job.getLocator().getPath();
            final String id = CommandLineTask.protect(path, "\"");

            String [] command = new String[] { oarCommand, "--stdout=oar.out", "--stderr=oar.err", id + ".run" };

            LOGGER.info("Running OAR with [%s]", command);

            XPMProcessBuilder processBuilder = connector.processBuilder();
            processBuilder.command(command);
            processBuilder.redirectOutput(Redirect.PIPE);
            processBuilder.redirectError(Redirect.PIPE);

            // START OAR and retrieves the process ID
            final XPMProcess process = processBuilder.start();

            final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String s;
            String pid = null;
            while ((s =reader.readLine()) != null) {
                if (s.startsWith(OARJOBID_PREFIX))
                    pid = s.substring(OARJOBID_PREFIX.length());
            }

            LOGGER.info("Started OAR job with PID %s", pid);

            return new OARProcess(job, pid, connector);
        }
    }



    /**
     * An OAR process
     */
    @Persistent
    private class OARProcess extends XPMProcess {
        /** Used for serialization */
        public OARProcess() {
        }

        public OARProcess(Job job, String pid, SingleHostConnector connector) {
            super(connector, String.format("oar:%s", connector.getHostName(), pid), job);
        }




        @Override
        public OutputStream getOutputStream() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public InputStream getInputStream() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public InputStream getErrorStream() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public int waitFor() throws InterruptedException {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void destroy() {
            //To change body of implemented methods use File | Settings | File Templates.
        }


        @Override
        public boolean isRunning() {
            final Document document = oarstat(false);
            String state = evaluateXPathToString("//item[@key = \"state\"]/text()", document);
            LOGGER.debug("State of OAR process %s is %s", pid, state);
            return "running".equalsIgnoreCase(state);
        }

        @Override
        public int exitValue() {
            final Document document = oarstat(true);
            String state = evaluateXPathToString("//item[@key = \"state\"]/text()", document);
            if ("running".equalsIgnoreCase(state))
                throw new IllegalThreadStateException("Job is running - cannot access its exit value");

            String code = evaluateXPathToString("//item[@key = \"exit_code\"]/text()", document);

            LOGGER.debug("Exit code of OAR process %s is %s", pid, code);

            if ("".equals(code))
                return -1;
            return Integer.parseInt(code);
        }

        /** Runs oarstat and returns the XML document */
        private Document oarstat(boolean full) {
            final Document document;
            try {
                document = exec(job, null, String.format("oarstat --xml --job %s %s", full ? "--full" : "", pid));
            } catch (Exception e) {
                throw new ExperimaestroRuntimeException(e, "Cannot parse oarstat output");
            }
            return document;
        }

    }
}
