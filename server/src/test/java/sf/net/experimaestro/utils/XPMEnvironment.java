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
import org.testng.annotations.BeforeClass;
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

    public static Scheduler scheduler;

    protected static TemporaryDirectory directory;

    /**
     * Make a directory corresponding to the caller
     *
     * @return
     */
    protected File mkTestDir() {
        StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
        // we get the caller method name
        StackTraceElement e = stacktrace[2];
        String methodName = e.getMethodName();
        assert directory != null;
        File jobDirectory = new File(directory.getFile(), methodName);

        jobDirectory.mkdirs();
        return jobDirectory;
    }


    @BeforeClass
    synchronized public void init() throws IOException {
        if (scheduler == null) {
            LOGGER.info("Opening scheduler [%s]", Thread.currentThread());
            directory = new TemporaryDirectory("scheduler-tests", "dir");
            final File dbFile = new File(directory.getFile(), "db");
            dbFile.mkdir();
            scheduler = new Scheduler(dbFile);
        }
    }


    @AfterClass
    public void close() {
    }


}
