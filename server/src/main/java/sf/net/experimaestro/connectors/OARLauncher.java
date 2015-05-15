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

import org.w3c.dom.Document;
import sf.net.experimaestro.exceptions.LaunchException;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.scripting.Expose;
import sf.net.experimaestro.manager.scripting.Exposed;
import sf.net.experimaestro.utils.Output;
import sf.net.experimaestro.utils.log.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystemException;
import java.nio.file.Path;

/**
 * A command line launcher with OAR
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Exposed
public class OARLauncher extends Launcher {
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
    @Expose
    public OARLauncher() {
        super();
    }

    /**
     * Helper method that executes a command that produces XML, and returns a DOM document from it
     */
    static Document exec(SingleHostConnector connector, String command) throws Exception {
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
    static String evaluateXPathToString(String expression, Document document) {
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
    public XPMScriptProcessBuilder scriptProcessBuilder(SingleHostConnector connector, Path scriptFile) throws IOException {
        final UnixScriptProcessBuilder unixScriptProcessBuilder = new UnixScriptProcessBuilder(scriptFile, connector, processBuilder(connector));
        unixScriptProcessBuilder.setNotificationURL(getNotificationURL());
        return unixScriptProcessBuilder;
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
        public XPMProcess start(boolean fake) throws LaunchException, IOException {
            if (fake) return null;
            final String path = job.getLocator();
            final String id = UnixScriptProcessBuilder.protect(path, "\"");

            String[] command = new String[]{oarCommand, "--stdout=oar.out", "--stderr=oar.err", id + ".run"};

            LOGGER.info("Running OAR with [%s]", Output.toString(" ", command));

            AbstractProcessBuilder processBuilder = connector.processBuilder();
            processBuilder.command(command);
            processBuilder.redirectOutput(Redirect.PIPE);
            processBuilder.redirectError(Redirect.PIPE);

            // START OAR and retrieves the process ID
            final XPMProcess process = processBuilder.start(fake);

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


}
