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

package sf.net.experimaestro.scheduler;

import com.sleepycat.persist.model.Persistent;
import sf.net.experimaestro.locks.FileLock;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.locks.UnlockableException;
import sf.net.experimaestro.utils.ProcessUtils;
import sf.net.experimaestro.utils.log.Logger;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 7/6/12
 */
@Persistent
public class LocalhostConnector extends Connector {
    static final private Logger LOGGER = Logger.getLogger();


    public LocalhostConnector() {
        super("local:");
    }

    @Override
    public PrintWriter printWriter(String identifier) throws Exception {
        return new PrintWriter(new File(identifier));
    }



    @Override
    public JobMonitor exec(Job job, String command, ArrayList<Lock> locks) throws Exception {
        Process p = null;
            LOGGER.info("Running command [%s]", command);
            p = Runtime.getRuntime().exec(new String[] { command });

            final Process finalP = p;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final BufferedReader reader = new BufferedReader(new InputStreamReader(finalP.getErrorStream()));
                    String s;
                    try {
                        while ((s = reader.readLine()) != null) {
                            LOGGER.error("[stderr] %s", s);
                        }
                    } catch (IOException e) {
                        LOGGER.error("[stderr/exception] %s", e.toString());
                    }
                }
            }).run();

            return new LocalhostJobMonitor(job, p);


//            synchronized (p) {
//                LOGGER.info("Waiting for the process (PID %d) to end", pid);
//                int code = -1;
//                try {
//                    code = p.waitFor();
//                } catch (InterruptedException e) {
//                    LOGGER.warn("Task has been interrupted");
//                }
//
//                if (code != 0)
//                    throw new RuntimeException(
//                            "Process ended with errors (code " + code + ")");
//
//                // Everything went well
//                LOGGER.info("Process (PID %d) ended without error", pid);
//                return code;
//            }
//        } finally {
//            if (p != null) {
//                p.getInputStream().close();
//                p.getOutputStream().close();
//                p.getErrorStream().close();
//            }
//        }
    }


    @Override
    public Lock createLockFile(String path) throws UnlockableException {
        return new FileLock(path);
    }

    @Override
    public void touchFile(String identifier) throws IOException {
        new File(identifier).createNewFile();
    }

    @Override
    public boolean fileExists(String identifier) {
        return new File(identifier).exists();
    }

    @Override
    public long getLastModifiedTime(String path) {
        return new File(path).lastModified();
    }

    @Override
    public InputStream getInputStream(String path) throws IOException {
        return new FileInputStream(path);
    }

    @Override
    public void renameFile(String from, String to) {
        new File(from).renameTo(new File(to));
    }

    @Override
    public void setExecutable(String path, boolean flag) {
        new File(path).setExecutable(flag);
    }


    @Override
    public int compareTo(Connector connector) {
        if (connector instanceof LocalhostConnector)
            return 0;

        return LocalhostConnector.class.getName().compareTo(connector.getClass().getName());
    }

    public static Connector getInstance() {
        return singleton;
    }

    static private Connector singleton = new LocalhostConnector();

    public static Locator getIdentifier(URI uri) {
        return new Locator(singleton, uri.getPath());
    }



    @Persistent
    private class LocalhostJobMonitor extends JobMonitor {

        transient private Process process;

        public LocalhostJobMonitor() {}

        // Check on Windows:
        // http://stackoverflow.com/questions/2318220/how-to-programmatically-detect-if-a-process-is-running-with-java-under-windows

        public LocalhostJobMonitor(Job job, Process p) {
            super(String.valueOf(ProcessUtils.getPID(p)), job);
            this.process = p;
        }

        @Override
        public int waitFor() throws Exception {
            if (process == null)
                return super.waitFor();
            return process.waitFor();
        }

        @Override
        boolean isRunning() throws Exception {
            if (process != null)
                return ProcessUtils.isRunning(process);

            return singleton.fileExists(job.identifier.path + Job.LOCK_EXTENSION);
        }

        @Override
        int getCode() throws Exception {
            if (singleton.fileExists(job.identifier.path + Job.DONE_EXTENSION))
                return -1;

            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }
}
