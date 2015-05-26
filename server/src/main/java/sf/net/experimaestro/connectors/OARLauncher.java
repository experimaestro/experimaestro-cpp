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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchService;
import java.util.ArrayList;

import static java.lang.String.format;

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

    /** The environment variable name corresponding to the job ID */
    public static final String OAR_JOB_ID = "OAR_JOB_ID";

    /** The hostname of the OAR node (necessary to reconnnect) */
    public static final String XPM_HOSTNAME = "XPM_HOSTNAME";

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
     * Short lived information
     */
    static private class ShortLivedInformation {
        /**
         * Hostname of the node
         */
        public String hostname;

        /**
         * The OAR job ID
         */
        String jobId;

        /**
         * End timestamp
         */
        long endTimestamp = 0;

        /**
         * Time (in seconds) of the short lived runner job (defaut: 1 hour)
         */
        long jobDuration = 60 * 60;

        /**
         * Time (in ms) that we need to run a short-lived process (default
         */
        long remainingTime = 10000;
    }

    /**
     * Process builder for OAR
     */
    public class ProcessBuilder extends AbstractProcessBuilder {

        /**
         * Is this a short-lived process?
         */
        boolean shortLived;

        String shortLivedJobDirectory = ".experimaestro/oar";

        // Command to start
        private String oarCommand = "oarsub";

        // The associated connector
        private SingleHostConnector connector;

        private ShortLivedInformation information;

        public ProcessBuilder(SingleHostConnector connector) {
            this.connector = connector;
        }

        public ProcessBuilder(SingleHostConnector connector, boolean shortLived) {
            this.connector = connector;
            this.shortLived = shortLived;
        }

        @Override
        public XPMProcess start(boolean fake) throws LaunchException, IOException {

            if (fake) return null;

            if (shortLived || !detach) {
                // Check if the process is still alive
                ensureShortLived();

                return null;
            } else {
                // Use a full
                final String path = job.getLocator();
                final String id = UnixScriptProcessBuilder.protect(path, "\"");

                ArrayList<String> command = new ArrayList<>();

                command.add(oarCommand);
                addOutputOption("stdout", command, output);
                addOutputOption("stderr", command, error);
                command.add(format("%s.run", id));

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

        protected void addOutputOption(String option, ArrayList<String> command, Redirect output) throws IOException {

            switch (output.type()) {
                case WRITE:
                    command.add(format("--%s=%s", option, connector.resolve(output.file())));
                case INHERIT:
                    command.add(format("--%s=/dev/null", option));
                default:
                    throw new UnsupportedOperationException(format("Cannot handle %s", output));
            }
        }

        /**
         * Ensure the short lived process is started
         */
        private void ensureShortLived() throws IOException, LaunchException {

            // Get the short lived job ending time
            if (information == null) {
                information = new ShortLivedInformation();
                // Create the directory if necessary
                final Path directory = connector.resolve(shortLivedJobDirectory);
                final Path infopath = directory.resolve("information.env");
                if (!Files.isDirectory(directory)) {
                    Files.createDirectories(directory);
                }

                // Retrieve information if present
                if (Files.exists(infopath)) {
                    try (InputStream input = Files.newInputStream(infopath);
                         BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                        String line;
                        while (((line = reader.readLine()) != null)) {
                            String[] fields = line.split("=", 1);
                            switch (fields[0]) {
                                case XPM_HOSTNAME:
                                    information.hostname = fields[1];
                                    break;
                                case OAR_JOB_ID:
                                    information.jobId = fields[1];
                                    break;

                            }
                        }
                    }
                }
            }


            // Starts the job if necessary
            long timestamp = System.currentTimeMillis();
            if ((information.remainingTime + timestamp) > information.endTimestamp) {
                // Launch a new OAR sub
                // The job outputs the environment and sleeps...
                String command = format("env; echo %s=$(hostname); sleep %d", XPM_HOSTNAME, information.jobDuration);

                final Path directory = connector.resolve(shortLivedJobDirectory);
                final Path infopath = directory.resolve("information.env");

                final AbstractProcessBuilder builder = connector.processBuilder();
                builder.command("oarsub", "--stdout=information.env", "--stderr=log.err",
                        format("--directory=%s", connector.resolve(directory)),
                        command);
                builder.detach(false);

                // Delete information file if it exists
                Files.deleteIfExists(infopath);
                final XPMProcess start = builder.start();

                // Wait that the job is launched
                final WatchService watchService = directory.getFileSystem().newWatchService();
                try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
                    directory.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);
                    while (!Files.exists(infopath)) {
                        LOGGER.debug("Waiting for information file %s", infopath);
                        watcher.take();
                    }
                } catch (NoSuchFileException | InterruptedException f) {
                    throw new RuntimeException(f);
                }

            }
        }


    }
}
