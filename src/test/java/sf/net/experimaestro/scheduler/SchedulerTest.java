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

import bpiwowar.argparser.utils.Output;
import com.sleepycat.je.DatabaseException;
import org.testng.annotations.Test;
import sf.net.experimaestro.connectors.XPMConnector;
import sf.net.experimaestro.exceptions.ExperimaestroCannotOverwrite;
import sf.net.experimaestro.utils.RandomSampler;
import sf.net.experimaestro.utils.ThreadCount;
import sf.net.experimaestro.utils.XPMEnvironment;
import sf.net.experimaestro.utils.log.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.lang.Math.*;

public class SchedulerTest extends XPMEnvironment {

    final static private Logger LOGGER = Logger.getLogger();


    @Test(/*timeOut = 1000,*/ description = "Run two jobs - one depend on the other to start")
    public void test_simple_dependency() throws
            DatabaseException, IOException, InterruptedException, ExperimaestroCannotOverwrite {

        File jobDirectory = mkTestDir();
        ThreadCount counter = new ThreadCount();

        // Create two jobs: job1, and job2 that depends on job1
        WaitingJob[] jobs = new WaitingJob[2];
        for (int i = 0; i < jobs.length; i++) {
            jobs[i] = new WaitingJob(scheduler, counter, jobDirectory, "job" + i, 500, 0);
            if (i > 0)
                jobs[i].addDependency(jobs[i - 1].createDependency(jobs[i - 1]));
            scheduler.store(jobs[i], false);
        }

        counter.resume();
        checkSequence(jobs);
        checkState(EnumSet.of(ResourceState.DONE), jobs);
    }


    @Test(/*timeOut = 1000,*/ description = "Run two jobs - one depend on the other to start, the first fails")
    public void test_failed_dependency() throws
            DatabaseException, IOException, InterruptedException, ExperimaestroCannotOverwrite {

        File jobDirectory = mkTestDir();
        ThreadCount counter = new ThreadCount();

        // Create two jobs: job1, and job2 that depends on job1
        WaitingJob[] jobs = new WaitingJob[2];
        for (int i = 0; i < jobs.length; i++) {
            jobs[i] = new WaitingJob(scheduler, counter, jobDirectory, "job" + i, 500, i == 0 ? 1 : 0);
            if (i > 0)
                jobs[i].addDependency(jobs[i - 1].createDependency(jobs[i - 1]));
            scheduler.store(jobs[i], false);
        }

        counter.resume();

        checkState(EnumSet.of(ResourceState.ERROR), jobs[0]);
        checkState(EnumSet.of(ResourceState.ON_HOLD), jobs[1]);
    }


    final static public class Link implements Comparable<Link> {
        int to, from;

        public Link(int to, int from) {
            this.to = to;
            this.from = from;
        }

        public Link(long n) {
            to = (int) floor(.5 + sqrt(2. * n + .25));
            from = (int) (n - (to * (to - 1)) / 2);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Link link = (Link) o;

            if (to != link.to) return false;
            if (from != link.from) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = to;
            result = 31 * result + from;
            return result;
        }

        @Override
        public int compareTo(Link o) {
            int z = Integer.compare(to, o.to);
            return z == 0 ? Integer.compare(from, o.from) : z;
        }
    }


    @Test(/*timeOut = 1000,*/ description = "Run jobs generated at random")
    public void test_complex_dependencies() throws ExperimaestroCannotOverwrite {
        // Number of jobs
        final int nbJobs = 20;

        final int nbCouples = nbJobs * (nbJobs - 1) / 2;

        // Number of dependencies (among possible ones)
        final double dependencyRatio = .2;
        // Maximum number of dependencies
        final int maxDependencies = min(200, nbCouples);

        // Failure ratio
        final double failureRatio = .05;
        // Minimum number of failures
        final int minFailures = 2;
        // Minimum job number for failure
        int minFailureId = 2;


        File jobDirectory = mkTestDir();
        ThreadCount counter = new ThreadCount();

        WaitingJob[] jobs = new WaitingJob[nbJobs];

        // --- Generate the dependencies
        TreeSet<Link> dependencies = new TreeSet<>();

        int n = min(min((int) (long) (nbCouples * dependencyRatio * random()), Integer.MAX_VALUE), maxDependencies);
        Random random = new Random();

        long[] values = new long[n];
        RandomSampler.sample(n, nbCouples, n, 0, values, 0, random);
        LOGGER.debug("Sampling %d values from %d", n, nbCouples);
        for (long v : values) {
            final Link link = new Link(v);
            dependencies.add(link);
            LOGGER.debug("LINK %d to %d [%d]", link.from, link.to, v);
            assert link.from < nbJobs;
            assert link.to < nbJobs;
            assert link.from < link.to;
        }

        // --- Select the jobs that will fail
        ResourceState[] states = new ResourceState[jobs.length];
        for (int i = 0; i < states.length; i++)
            states[i] = ResourceState.DONE;
        n = (int) max(minFailures, random() * failureRatio * jobs.length);
        long[] values2 = new long[n];
        RandomSampler.sample(n, jobs.length - minFailureId, n, minFailureId, values2, 0, random);
        for (int i = 0; i < n; i++)
            states[((int) values2[i])] = ResourceState.ERROR;


        // --- Generate new jobs
        for (int i = 0; i < jobs.length; i++) {

            int waitingTime = random.nextInt(500) + 50;
            jobs[i] = new WaitingJob(scheduler, counter, jobDirectory, "job" + i, waitingTime, states[i] == ResourceState.DONE ? 0 : 1);

            ArrayList<String> deps = new ArrayList<>();
            for (Link link : dependencies.subSet(new Link(i, 0), true, new Link(i, Integer.MAX_VALUE), true)) {
                assert i == link.to;
                jobs[i].addDependency(jobs[link.from].createDependency(null));
                if (states[i] != ResourceState.ERROR && states[link.from].isBlocking())
                    states[i] = ResourceState.ON_HOLD;
                deps.add(jobs[link.from].toString());

            }

            scheduler.store(jobs[i], false);
            LOGGER.debug("Job [%s] created: final=%s, deps=%s", jobs[i], states[i], Output.toString(", ", deps));
        }

        LOGGER.info("Waiting for jobs to finish");
        counter.resume();

        // --- Check
        for (Link link : dependencies) {
            if (states[link.from] == ResourceState.DONE && jobs[link.to].state == ResourceState.DONE)
                checkSequence(jobs[link.from], jobs[link.to]);
        }
        for (int i = 0; i < jobs.length; i++)
            checkState(EnumSet.of(states[i]), jobs[i]);
    }


    @Test(/*timeOut = 5000,*/ description = "Test of the token resource - one job at a time")
    public void test_token_resource() throws ExperimaestroCannotOverwrite {

        File jobDirectory = mkTestDir();

        ThreadCount counter = new ThreadCount();
        ResourceLocator locator = new ResourceLocator(XPMConnector.getInstance(), "test");
        TokenResource token = new TokenResource(scheduler, new ResourceData(locator), 1);
        scheduler.store(token, false);

        WaitingJob[] jobs = new WaitingJob[2];
        for (int i = 0; i < jobs.length; i++) {
            jobs[i] = new WaitingJob(scheduler, counter, jobDirectory, "job" + i, 500, 0);
            jobs[i].addDependency(token.createDependency(null));
            scheduler.store(jobs[i], false);
        }


        counter.resume();

        // Check that one started after the other (since only one must have been active
        // at a time)
        Arrays.sort(jobs, new Comparator<WaitingJob>() {
            @Override
            public int compare(WaitingJob o1, WaitingJob o2) {
                return Long.compare(o1.getStartTimestamp(), o2.getStartTimestamp());
            }
        });

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
    static private void checkSequence(WaitingJob... jobs) {
        for (int i = 0; i < jobs.length - 1; i++) {
            assert jobs[i].getEndTimestamp() < jobs[i + 1].readyTimestamp
                    : String.format("The jobs (%s, end=%d) and (%s, start=%d) did not start one after the other",
                    jobs[i], jobs[i].getEndTimestamp(),
                    jobs[i + 1], jobs[i + 1].readyTimestamp);

            // just to be on the safe side
            assert jobs[i].getEndTimestamp() < jobs[i + 1].getStartTimestamp();
        }

    }

    /**
     * Check the jobs states
     *
     * @param states The state
     * @param jobs   The jobs
     */
    private void checkState(EnumSet<ResourceState> states, WaitingJob... jobs) {
        for (int i = 0; i < jobs.length; i++)
            assert states.contains(jobs[i].state)
                    : String.format("The job (%s) is not in one of the states %s but %s", jobs[i], states, jobs[i].state);

    }

}
