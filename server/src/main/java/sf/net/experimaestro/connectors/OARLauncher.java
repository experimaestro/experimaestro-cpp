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

package sf.net.experimaestro.connectors;

import com.sleepycat.persist.model.Persistent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.w3c.dom.Document;
import sf.net.experimaestro.exceptions.LaunchException;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.scheduler.Job;
import sf.net.experimaestro.utils.Output;
import sf.net.experimaestro.utils.log.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;

/**
 * A command line launcher with OAR
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Persistent
public class OARLauncher implements Launcher {
    /**
     * Prefix for the PID of the job
     */
    protected static final String OARJOBID_PREFIX = "OAR_JOB_ID=";
    static private final Logger LOGGER = Logger.getLogger();
    /**
     * oarsub command
     */
    private String oarCommand = "oarsub";

    /**
     * Construction from a connector
     */
    public OARLauncher() {
        super();
    }

    /**
     * Helper method that executes a command that produces XML, and returns a DOM document from it
     */
    static private Document exec(SingleHostConnector connector, String command) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

        AbstractProcessBuilder builder = connector.processBuilder();
        builder.command(command);
        builder.detach(false);
        return dBuilder.parse(builder.start().getInputStream());
    }

    /**
     * Evaluate an XPath to a string
     */
    static private String evaluateXPathToString(String expression, Document document) {
        String value;
        XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            value = (String) xpath.evaluate(expression, document,
                    XPathConstants.STRING);
        } catch (XPathExpressionException e) {
            throw new XPMRuntimeException(e, "Cannot evaluted XPath expression [%s]", xpath);
        }
        return value;
    }

    @Override
    public AbstractProcessBuilder processBuilder(SingleHostConnector connector) throws FileSystemException {
        return new ProcessBuilder(connector);
    }

    @Override
    public XPMScriptProcessBuilder scriptProcessBuilder(SingleHostConnector connector, FileObject scriptFile) throws FileSystemException {
        return new UnixScriptProcessBuilder(scriptFile, connector, processBuilder(connector));
    }

    /**
     * Process builder for OAR
     */
    static public class ProcessBuilder extends AbstractProcessBuilder {
        // Command to start
        private String oarCommand = "oarsub";

        // The associated connector
        private SingleHostConnector connector;

        public ProcessBuilder(SingleHostConnector connector) {
            this.connector = connector;
        }

        @Override
        public XPMProcess start() throws LaunchException, IOException {
            final String path = job.getLocator().getPath();
            final String id = UnixScriptProcessBuilder.protect(path, "\"");

            String[] command = new String[]{oarCommand, "--stdout=oar.out", "--stderr=oar.err", id + ".run"};

            LOGGER.info("Running OAR with [%s]", Output.toString(" ", command));

            AbstractProcessBuilder processBuilder = connector.processBuilder();
            processBuilder.command(command);
            processBuilder.redirectOutput(Redirect.PIPE);
            processBuilder.redirectError(Redirect.PIPE);

            // START OAR and retrieves the process ID
            final XPMProcess process = processBuilder.start();

            final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String s;
            String pid = null;
            while ((s = reader.readLine()) != null) {
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
    static private class OARProcess extends XPMProcess {
        /**
         * Used for serialization
         */
        public OARProcess() {
        }

        public OARProcess(Job job, String pid, SingleHostConnector connector) {
            super(connector, String.format("oar:%s", connector.getHostName(), pid), job);
            startWaitProcess();
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
            String state = evaluateXPathToString("//item[@identifier = \"state\"]/text()", document);
            LOGGER.debug("State of OAR process %s is %s", pid, state);
            return "running".equalsIgnoreCase(state);
        }

        @Override
        public int exitValue() {
            final Document document = oarstat(true);
            String state = evaluateXPathToString("//item[@identifier = \"state\"]/text()", document);
            if ("running".equalsIgnoreCase(state))
                throw new IllegalThreadStateException("Job is running - cannot access its exit value");

            String code = evaluateXPathToString("//item[@identifier = \"exit_code\"]/text()", document);

            LOGGER.debug("Exit code of OAR process %s is %s", pid, code);

            if ("".equals(code))
                return -1;
            return Integer.parseInt(code);
        }

        /**
         * Runs oarstat and returns the XML document
         */
        private Document oarstat(boolean full) {
            final Document document;
            try {
                document = exec(connector, String.format("oarstat --xml --job %s %s", full ? "--full" : "", pid));
            } catch (Exception e) {
                throw new XPMRuntimeException(e, "Cannot parse oarstat output");
            }
            return document;
        }

    }
}
