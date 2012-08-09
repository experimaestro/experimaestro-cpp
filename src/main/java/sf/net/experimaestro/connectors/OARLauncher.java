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
import sf.net.experimaestro.exceptions.ExperimaestroException;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.scheduler.CommandLineTask;
import sf.net.experimaestro.scheduler.Job;
import sf.net.experimaestro.scheduler.UnixShellLauncher;
import sf.net.experimaestro.utils.log.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
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

    @Override
    public sf.net.experimaestro.connectors.XPMProcess launch(CommandLineTask job, ArrayList<Lock> locks) throws Exception {
        generateRunFile(job, locks);

        final String path = job.getLocator().getPath();
        final String id = CommandLineTask.protect(path, "\"");
        String command = String.format("%s --stdout=\"%s.out\" --stderr=\"%2$s.err\" \"%2$s.run\" ",
                oarCommand, id);

        LOGGER.info("Running OAR with [%s]", command);

        final sf.net.experimaestro.scheduler.XPMProcess process = job.getConnector().exec(job, command, locks, false, null, null);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String s;
        String pid = null;
        while ((s =reader.readLine()) != null) {
            if (s.startsWith(OARJOBID_PREFIX))
                pid = s.substring(OARJOBID_PREFIX.length());
        }

        LOGGER.info("Started OAR job with PID %s", pid);

        return new OARJobMonitor(job, pid);
    }

    /**
     * Executes a command that produces XML, and returns a DOM document from it
     */
    private static Document exec(Job job, ArrayList<Lock> locks, String command) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        final sf.net.experimaestro.scheduler.XPMProcess process = job.getConnector().exec(job, command, locks, false, null, null);
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
            throw new ExperimaestroException(e, "Cannot evaluted XPath expression [%s]", xpath);
        }
        return value;
    }


    /**
     * Monitors a OAR process
     */
    @Persistent
    private class OARJobMonitor extends sf.net.experimaestro.connectors.XPMProcess {
        public OARJobMonitor() {
        }

        public OARJobMonitor(Job job, String pid) {
            super(job, pid);
        }

        @Override
        public boolean isRunning() {
            final Document document = oarstat(false);
            String state = evaluateXPathToString("//item[@key = \"state\"]/text()", document);
            LOGGER.debug("State of OAR process %s is %s", pid, state);
            return "running".equalsIgnoreCase(state);
        }

        @Override
        int getCode() {
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

        private Document oarstat(boolean full) {
            final Document document;
            try {
                document = exec(job, null, String.format("oarstat --xml --job %s %s", full ? "--full" : "", pid));
            } catch (Exception e) {
                throw new ExperimaestroException(e, "Cannot parse oarstat output");
            }
            return document;
        }

    }
}
