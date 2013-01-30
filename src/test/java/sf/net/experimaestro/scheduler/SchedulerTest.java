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

package sf.net.experimaestro.scheduler;

import com.sleepycat.je.DatabaseException;
import org.testng.annotations.Test;
import sf.net.experimaestro.connectors.XPMConnector;
import sf.net.experimaestro.exceptions.ExperimaestroCannotOverwrite;
import sf.net.experimaestro.utils.ThreadCount;
import sf.net.experimaestro.utils.XPMEnvironment;
import sf.net.experimaestro.utils.log.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;

public class SchedulerTest extends XPMEnvironment {

    final static private Logger LOGGER = Logger.getLogger();


    @Test(timeOut = 1000, description = "Run two jobs - one depend on the other to start")
    public void test_simple_dependency() throws
            DatabaseException, IOException, InterruptedException, ExperimaestroCannotOverwrite {

        File jobDirectory = mkTestDir();
        ThreadCount counter = new ThreadCount();

        // Create two jobs: job1, and job2 that depends on job1
        WaitingJob[] jobs = new WaitingJob[2];
        for (int i = 0; i < jobs.length; i++) {
            jobs[i] = new WaitingJob(scheduler, counter, jobDirectory, "job" + i, 500);
            if (i > 1)
                jobs[i].addDependency(jobs[i - 1].createDependency(jobs[i - 1]));
            scheduler.store(jobs[i]);
        }

        counter.resume();
        checkSequence(jobs);
        checkState(EnumSet.of(ResourceState.DONE), jobs);
    }




    @Test(timeOut = 5000, description = "Test of the token resource - one job at a time")
    public void test_token_resource() throws ExperimaestroCannotOverwrite {

        File jobDirectory = mkTestDir();

        ThreadCount counter = new ThreadCount();
        ResourceLocator locator = new ResourceLocator(XPMConnector.getInstance(), "test");
        TokenResource token = new TokenResource(scheduler, new ResourceData(locator), 1);
        scheduler.store(token);

        WaitingJob[] jobs = new WaitingJob[2];
        for (int i = 0; i < jobs.length; i++) {
            jobs[i] = new WaitingJob(scheduler, counter, jobDirectory, "job" + i, 500);
            jobs[i].addDependency(token.createDependency(null));
            scheduler.store(jobs[i]);
        }


        counter.resume();

        Arrays.sort(jobs, new Comparator<WaitingJob>() {
            @Override
            public int compare(WaitingJob o1, WaitingJob o2) {
                return Long.compare(o1.getStartTimestamp(), o2.getStartTimestamp());
            }
        });

        // Check that one started after the other
        checkSequence(jobs);
        checkState(EnumSet.of(ResourceState.DONE), jobs);
    }


    // ----- Utility methods for scheduler

    /**
     * Make a directory corresponding to the caller
     *
     * @return
     */
    private File mkTestDir() {
        StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
        // we get the caller method name
        StackTraceElement e = stacktrace[2];
        String methodName = e.getMethodName();
        File jobDirectory = new File(directory.getFile(), methodName);

        jobDirectory.mkdirs();
        return jobDirectory;
    }


    /**
     * Check that these jobs started one after the other
     *
     * @param jobs
     */
    static private void checkSequence(Job<?>... jobs) {
        for (int i = 0; i < jobs.length - 1; i++)
            assert jobs[i].getEndTimestamp() < jobs[i + 1].getStartTimestamp()
                    : String.format("The jobs (%s) and (%s) did not start one after the other", jobs[i], jobs[i + 1]);
    }

    /**
     * Check the jobs states
     *
     * @param states The state
     * @param jobs   The jobs
     */
    private void checkState(EnumSet<ResourceState> states, Job[] jobs) {
        for (int i = 0; i < jobs.length; i++)
            assert states.contains(jobs[i].state)
                    : String.format("The job (%s) is not in one of the states %s", jobs[i], states);

    }

}
