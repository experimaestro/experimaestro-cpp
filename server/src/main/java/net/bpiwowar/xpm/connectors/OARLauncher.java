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

import net.bpiwowar.xpm.commands.Redirect;
import net.bpiwowar.xpm.commands.UnixScriptProcessBuilder;
import net.bpiwowar.xpm.commands.XPMScriptProcessBuilder;
import org.w3c.dom.Document;
import net.bpiwowar.xpm.exceptions.LaunchException;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.exceptions.XPMScriptRuntimeException;
import net.bpiwowar.xpm.manager.scripting.Expose;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.commands.Commands;
import net.bpiwowar.xpm.scheduler.LauncherParameters;
import net.bpiwowar.xpm.scheduler.Resource;
import net.bpiwowar.xpm.utils.Output;
import net.bpiwowar.xpm.utils.log.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static net.bpiwowar.xpm.utils.PathUtils.QUOTED_SPECIAL;
import static net.bpiwowar.xpm.utils.PathUtils.SHELL_SPECIAL;
import static net.bpiwowar.xpm.utils.PathUtils.protect;

/**
 * A command line launcher with OAR
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Exposed
public class OARLauncher extends Launcher {

    /**
     * The environment variable name corresponding to the job ID
     */
    public static final String OAR_JOB_ID = "OAR_JOB_ID";

    /**
     * The hostname of the OAR node (necessary to reconnnect)
     */
    public static final String XPM_HOSTNAME = "XPM_HOSTNAME";

    public static final String XPM_STARTTIME = "XPM_STARTTIME";

    public static final String XPM_SLEEPTIME = "XPM_SLEEPTIME";

    /**
     * Prefix for the PID of the job
     */
    protected static final String OARJOBID_PREFIX = "OAR_JOB_ID=";

    static private final Logger LOGGER = Logger.getLogger();
    private final String oarUser = "oar";
    private final int oarPort = 6667;

    /**
     * oarsub command
     */
    private String oarCommand = "oarsub";

    private String oarshCommand = "oarsh";

    private String unixtimestampCommand = "date +%s";

    /**
     * The information
     */
    private boolean useNotify = false;

    /**
     * Use job key
     */
    private boolean useJobKey = true;

    private ShortLivedInformation information;

    /**
     * Construction from a connector
     */
    @Expose
    public OARLauncher(Connector connector) {
        super(connector);
    }

    /**
     * Helper method that executes a command that produces XML, and returns a DOM document from it
     */
    static Document exec(SingleHostConnector connector, String... command) throws Exception {

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
    public AbstractProcessBuilder processBuilder() throws FileSystemException {
        return new ProcessBuilder(connector.getMainConnector());
    }

    @Override
    public XPMScriptProcessBuilder scriptProcessBuilder(Path scriptFile, LauncherParameters parameters) throws IOException {
        final UnixScriptProcessBuilder unixScriptProcessBuilder = new UnixScriptProcessBuilder(scriptFile, this, processBuilder());
        unixScriptProcessBuilder.setNotificationURL(getNotificationURL());
        unixScriptProcessBuilder.environment(environment);
        if (useNotify) {
            unixScriptProcessBuilder.setDoCleanup(false);
            Commands commands = new Commands();
            commands.addUnprotected("case \"$3\" in");
            commands.addUnprotected("END) ERROR) cleanup;;");
            commands.addUnprotected("*) exit;;");
            commands.addUnprotected("esac");
            unixScriptProcessBuilder.preprocessCommands(commands);
        }

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
         * Time (in s) that we need to run a short-lived process (default 100s)
         */
        long remainingTime = 100;

        /**
         * Direct (SSH) connector instead of using oarsh
         */
        SingleHostConnector connector;

        /**
         * Wall time in hh:mm:ss format
         * Adds a few seconds
         *
         * @return
         */
        public String oarSpecification() {
            long hours = this.jobDuration + 30;

            long seconds = hours % 60;
            hours /= 60;

            long minutes = hours % 60;
            hours /= 60;

            return format("nodes=1/core=1,walltime=%d:%02d:%02d", hours, minutes, seconds);
        }
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

        // The associated connector
        private SingleHostConnector connector;

        public ProcessBuilder(SingleHostConnector connector) {
            this(connector, false);
        }

        public ProcessBuilder(SingleHostConnector connector, boolean shortLived) {
            this.connector = connector;
            this.shortLived = shortLived;
            useJobKey = connector instanceof SSHConnector;
        }

        @Override
        public XPMProcess start(boolean fake) throws LaunchException, IOException {

            if (fake) return null;

            if (shortLived || !detach) {
                // Check if the process is still alive
                ensureShortLived();

                final AbstractProcessBuilder builder = useJobKey ? information.connector.processBuilder() : connector.processBuilder();

                ArrayList<String> command = new ArrayList<>();
                if (!useJobKey) {
                    Map<String, String> env = new HashMap<>();
                    env.put(OAR_JOB_ID, information.jobId);
                    builder.environment(env);

                    command.add(OAR_JOB_ID + "=" + information.jobId);
                    command.add(oarshCommand);
                    command.add(information.hostname);
                    if (OARLauncher.this.environment != null) {
                        OARLauncher.this.environment.forEach(
                                (k, v) -> command.add(format("export %s=%s;", k, protect(v, QUOTED_SPECIAL)))
                        );
                    }
                    if (environment() != null) {
                        environment().forEach(
                                (k, v) -> command.add(format("export %s=%s;", k, protect(v, QUOTED_SPECIAL)))
                        );
                    }
                } else {
                    HashMap<String, String> environment = new HashMap<>();
                    if (OARLauncher.this.environment != null) {
                        environment.putAll(OARLauncher.this.environment);
                    }
                    if (environment() != null) {
                        environment.putAll(environment());
                    }

                    builder.environment(environment);
                }


                this.command().stream().forEach(command::add);
                builder.command(command);

                builder.detach(false);

                if (input != null) builder.redirectInput(input);
                if (error != null) builder.redirectError(error);
                if (output != null) builder.redirectOutput(output);

                final XPMProcess process = builder.start();
                return process;

            } else {
                // Use a full OAR process

                final String path = connector.resolve(Resource.RUN_EXTENSION.transform(job.getLocator()));
                final String runpath = protect(path, SHELL_SPECIAL);

                ArrayList<String> command = new ArrayList<>();

                command.add(oarCommand);
                if (useNotify) {
                    command.add(format("--exec:%s", path));
                }
                addOutputOption("stdout", command, output);
                addOutputOption("stderr", command, error);
                command.add(runpath);

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
                LOGGER.info("Got PID %s", pid);

                final int code;
                try {
                    code = process.waitFor();
                    if (code != 0) {
                        throw new LaunchException("Error while launching oar job [%d]", code);
                    }
                } catch (InterruptedException e) {
                    throw new LaunchException(e, "Error while launching oar job");
                }

                LOGGER.info("Started OAR job with PID %s", pid);

                return new OARProcess(job, pid, connector);
            }
        }

        protected void addOutputOption(String option, ArrayList<String> command, Redirect output) throws IOException {

            switch (output.type()) {
                case WRITE:
                    command.add(format("--%s=%s", option, connector.resolve(output.file())));
                    break;
                case INHERIT:
                    command.add(format("--%s=/dev/null", option));
                    break;
                default:
                    throw new UnsupportedOperationException(format("Cannot handle %s", output));
            }
        }

        /**
         * Ensure the short lived process is started
         * <p>
         * The following steps are followed:
         * <ol>
         * <li>Read the information file if present. If enough time remains, exits.</li>
         * <li>Kill the previous OAR job</li>
         * <li>Creates a lock file</li>
         * <li>Launch the OAR sleeping job</li>
         * <li>Waits that it is launched</li>
         * </ol>
         */
        // TODO: Should open an SSH session (for this, we need to get the environment variable set by oarsh)
        private void ensureShortLived() throws IOException, LaunchException {
            // Get the short lived job ending time
            final Path directory = connector.resolve(shortLivedJobDirectory);
            final Path jobKeyFile = directory.resolve("information.key");

            if (information == null) {
                information = new ShortLivedInformation();
                // Create the directory if necessary
                final Path infopath = directory.resolve("information.env");
                if (!Files.isDirectory(directory)) {
                    Files.createDirectories(directory);
                }

                readInformation(infopath, useJobKey ? jobKeyFile : null);
            }


            // Starts the job if necessary
            long timestamp = System.currentTimeMillis() / 1000;
            if ((information.remainingTime + timestamp) > information.endTimestamp) {
                final Path infopath = directory.resolve("information.env");
                final Path commandpath = directory.resolve("command.sh");
                final Path lockPath = directory.resolve("information.lock");
                final Path donePath = directory.resolve("information.done");

                // Kill old job
                if (information.jobId != null && Files.exists(lockPath)) {
                    final OARStat oarStat = new OARStat(connector, information.jobId, false);
                    if (!oarStat.isRunning()) {
                        Files.delete(lockPath);
                    } else {
                        LOGGER.info("Killing old job [%s]", information.jobId);
                        final AbstractProcessBuilder builder = connector.processBuilder();
                        builder.command("oardel", information.jobId);
                        builder.detach(false);
                        final XPMProcess process = builder.start();
                        try {

                            int code = process.waitFor();
                            if (code != 0) {
                                throw new XPMScriptRuntimeException("Error while killing old OAR job: %d", code);
                            }
                        } catch (InterruptedException e) {
                            LOGGER.error(e, "Waiting interrupted");
                        }
                    }
                }

                // Wait for lockfile to be removed
                LOGGER.info("Waiting for old lock file to be removed");
                // Wait that the job is launched
                try (WatchService watcher = directory.getFileSystem().newWatchService()) {
                    directory.register(watcher, StandardWatchEventKinds.ENTRY_DELETE);
                    while (Files.exists(lockPath)) {
                        LOGGER.debug("Waiting for lock file to be removed %s", lockPath);
                        watcher.poll(1, TimeUnit.SECONDS);
                    }
                } catch (NoSuchFileException | InterruptedException f) {
                    throw new RuntimeException(f);
                }

                // Create lock
                Files.createFile(lockPath);

                // Writes file
                try (BufferedWriter writer = Files.newBufferedWriter(commandpath)) {
                    writer.write(format("cleanup() {\n echo Removing lock file 1>&2\nrm %s\n}\n",
                            connector.quotedPath(lockPath)));
                    writer.write("trap cleanup 0\n");
                    writer.write("env\n");
                    writer.write(format("echo %s=$(hostname)%n", XPM_HOSTNAME));
                    writer.write(format("echo %s=%s%n", XPM_SLEEPTIME, information.jobDuration));
                    writer.write(format("echo %s=$(%s)%n", XPM_STARTTIME, unixtimestampCommand));
                    writer.write(format("touch %s%n", connector.quotedPath(donePath)));
                    writer.write(format("sleep %d%n", information.jobDuration));
                }
                Files.setPosixFilePermissions(commandpath, PosixFilePermissions.fromString("rwxr-x---"));


                // Launch a new OAR sub
                // The job outputs the environment and sleeps...

                Files.deleteIfExists(donePath);
                final AbstractProcessBuilder builder = connector.processBuilder();
                ArrayList<String> args = new ArrayList<>();
                args.addAll(Arrays.asList(
                        oarCommand, "-l", information.oarSpecification(),
                        "--stdout=information.env", "--stderr=log.err",
                        format("--directory=%s", connector.resolve(directory))
                ));

                if (useJobKey) {
                    args.add("--use-job-key");
                    args.add(format("--export-job-key-to-file=%s", connector.resolve(jobKeyFile)));
                }

                args.add(connector.resolve(commandpath));

                builder.command(args);
                builder.detach(false);
                final XPMProcess process = builder.start();
                try {
                    int code = process.waitFor();
                    if (code != 0) {
                        Files.delete(lockPath);
                        throw new XPMScriptRuntimeException("Error while starting OAR job: code %d", code);
                    }
                } catch (InterruptedException e) {
                    LOGGER.error(e, "Waiting interrupted");
                }

                // Wait that the job is launched
                try (WatchService watcher = directory.getFileSystem().newWatchService()) {
                    directory.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);
                    while (!Files.exists(donePath)) {
                        LOGGER.debug("Waiting for information file to be generated [%s]", donePath);
                        watcher.poll(1, TimeUnit.SECONDS);
                    }
                    readInformation(infopath, useJobKey ? jobKeyFile : null);
                } catch (NoSuchFileException | InterruptedException f) {
                    throw new RuntimeException(f);
                }

            }
        }

        /**
         * Read information about the OAR sleeping job
         *
         * @param infopath   The path to the file containing the information
         * @param jobKeyPath
         * @throws IOException If an error occurs while reading the file
         */
        private void readInformation(Path infopath, Path jobKeyPath) throws IOException {
            // Retrieve information if present
            if (Files.exists(infopath)) {
                try (InputStream input = Files.newInputStream(infopath);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                    String line;
                    while (((line = reader.readLine()) != null)) {
                        String[] fields = line.split("=", 2);
                        switch (fields[0]) {
                            case XPM_HOSTNAME:
                                information.hostname = fields[1];
                                break;
                            case OAR_JOB_ID:
                                information.jobId = fields[1];
                                break;
                            case XPM_SLEEPTIME:
                                information.jobDuration = Long.parseLong(fields[1]);
                            case XPM_STARTTIME:
                                information.endTimestamp = Long.parseLong(fields[1]);
                        }
                    }
                }
                information.endTimestamp += information.jobDuration;

                // Retrieve job key if generating it
                if (jobKeyPath != null) {
                    byte[] key = Files.readAllBytes(jobKeyPath);
                    SSHOptions options = new SSHOptions();
                    options.setStreamProxy((SSHConnector) connector);
                    options.setPrivateKey("oarkey", key);
                    options.strictHostChecking(false);
                    options.setUseSSHAgent(false);
                    information.connector = new SSHConnector(oarUser, information.hostname, oarPort, options);
                }
            }
        }


    }

    @Expose("use_notify")
    public void setUseNotify(boolean useNotify) {
        this.useNotify = useNotify;
    }

    @Expose
    public OARParameters parameters() {
        return new OARParameters(this);
    }
}
