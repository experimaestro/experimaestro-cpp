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
import sf.net.experimaestro.utils.log.Logger;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 7/6/12
 */
@Persistent
public class LocalhostConnector implements Connector {
    static final private Logger LOGGER = Logger.getLogger();

    @Override
    public PrintWriter printWriter(String identifier) throws Exception {
        return new PrintWriter(new File(identifier));
    }

    @Override
    public int exec(String command, ArrayList<Lock> locks) throws Exception {
        Process p = null;
        try {
            LOGGER.info("Running command [%s]", command);
            p = Runtime.getRuntime().exec(new String[] {
                    "/bin/bash", "-c", command
            }
            );

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

            // Changing the ownership of the different logs
            final int pid = sf.net.experimaestro.utils.PID.getPID(p);
            for (Lock lock : locks) {
                lock.changeOwnership(pid);
            }

            synchronized (p) {
                LOGGER.info("Waiting for the process (PID %d) to end", pid);
                int code = -1;
                try {
                    code = p.waitFor();
                } catch (InterruptedException e) {
                    LOGGER.warn("Task has been interrupted");
                }

                if (code != 0)
                    throw new RuntimeException(
                            "Process ended with errors (code " + code + ")");

                // Everything went well
                LOGGER.info("Process (PID %d) ended without error", pid);
                return code;
            }
        } finally {
            if (p != null) {
                p.getInputStream().close();
                p.getOutputStream().close();
                p.getErrorStream().close();
            }
        }
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
    public String getIdentifier() {
        return "local:";
    }

    public static Connector getInstance() {
        return singleton;
    }

    static private Connector singleton = new LocalhostConnector();

    public static Identifier getIdentifier(URI uri) {
        return new Identifier(singleton, uri.getPath());
    }
}
