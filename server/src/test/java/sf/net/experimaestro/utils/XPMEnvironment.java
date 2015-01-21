package sf.net.experimaestro.utils;

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

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.log.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Ensures that only one scheduler is opened throughout the tests
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class XPMEnvironment {
    private static final Logger LOGGER = Logger.getLogger();

    static final private Integer token = 0;

    private static Scheduler scheduler;

    private static TemporaryDirectory directory;

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

    public static Scheduler getScheduler() throws IOException {
        synchronized (token) {
            if (scheduler == null) {
                LOGGER.info("Opening scheduler [%s]", Thread.currentThread());
                final File dbFile = new File(getDirectory().getFile(), "db");
                dbFile.mkdir();
                scheduler = new Scheduler(dbFile);
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    LOGGER.info("Stopping scheduler");
                    scheduler.close();
                    LOGGER.info("Scheduler stopped");
                }));
            }
        }
        return scheduler;
    }
}
