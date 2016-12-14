package net.bpiwowar.xpm.utils;

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

import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.lang.RandomStringUtils;
import net.bpiwowar.xpm.tasks.ServerTask;
import net.bpiwowar.xpm.utils.log.Logger;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

import static java.lang.String.format;

/**
 * Ensures that only one scheduler is opened throughout the tests
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class XPMEnvironment {
    private static final Logger LOGGER = Logger.getLogger();

    static final private Integer token = 0;

    private static ServerTask server;

    public static String testUser = "test";

    public static String testPassword;

    private static TemporaryDirectory directory;

    /**
     * Finds a free local socket port.
     *
     * @return a free local socket port.
     * @throws IOException
     */
    public static int findFreeLocalPort() throws IOException {

        ServerSocket server = new ServerSocket(0);
        try {
            return server.getLocalPort();
        } finally {
            server.close();
        }
    }

    /**
     * Make a directory corresponding to the caller
     *
     * @return
     */
    protected File mkTestDir() throws IOException {
        StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
        // we get the caller method name
        StackTraceElement e = stacktrace[2];
        String methodName = e.getMethodName();
        File jobDirectory = new File(getDirectory().getFile(), methodName);

        jobDirectory.mkdirs();
        return jobDirectory;
    }

    public static TemporaryDirectory getDirectory() throws IOException {
        if (directory != null) {
            return directory;
        }

        synchronized (token) {
            if (directory == null) {
                directory = new TemporaryDirectory("scheduler-tests", "dir");
            }
        }
        return directory;
    }

    public static ServerTask prepare() throws Throwable {
        synchronized (token) {
            if (server == null) {
                LOGGER.info("Opening scheduler [%s]", Thread.currentThread());
                final TemporaryDirectory tmpDirectory = getDirectory();
                final File mainDirectory = tmpDirectory.getFile();

                final File dbFile = new File(mainDirectory, "db");
                dbFile.mkdir();

                server = new ServerTask();


                HierarchicalINIConfiguration serverConfiguration = new HierarchicalINIConfiguration();
                serverConfiguration.setProperty("server.database", dbFile.getAbsolutePath());
                serverConfiguration.setProperty("server.name", "test");
                serverConfiguration.setProperty("server.port", findFreeLocalPort());
                testPassword = RandomStringUtils.randomAlphanumeric(10);
                serverConfiguration.setDelimiterParsingDisabled(true);
                serverConfiguration.setProperty("passwords." + testUser, format("%s, user", testPassword));

                // Just to avoid reading the default configuration file
                serverConfiguration.setFile(mainDirectory);

                server.setConfiguration(serverConfiguration);

                server.wait(false); // No need to wait
                server.execute();

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    LOGGER.info("Stopping server");
                    try {
                        server.close();
                        tmpDirectory.close();
                    } catch (Exception e) {
                        LOGGER.error(e, "Could not close the server");
                    }
                    LOGGER.info("Scheduler server");
                }));

                tmpDirectory.setAutomaticDelete(false);

            }
        }
        return server;
    }
}
