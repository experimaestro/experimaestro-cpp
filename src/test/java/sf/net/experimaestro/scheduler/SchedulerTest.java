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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import sf.net.experimaestro.connectors.XPMConnector;
import sf.net.experimaestro.exceptions.ExperimaestroCannotOverwrite;
import sf.net.experimaestro.utils.RandomSampler;
import sf.net.experimaestro.utils.ThreadCount;
import sf.net.experimaestro.utils.XPMEnvironment;
import sf.net.experimaestro.utils.log.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Random;
import java.util.TreeSet;

import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;

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


    static public class ComplexDependenciesParameters {
        String name;
        Long seed = null;

        int maxExecutionTime = 50;
        int minExecutionTime = 10;

        // Number of jobs
        int nbJobs = 20;


        // Number of dependencies (among possible ones)
        double dependencyRatio = .2;

        // Maximum number of dependencies
        int maxDeps = 200;

        // Failure ratio
        double failureRatio = .05;

        // Minimum number of failures
        int minFailures = 2;

        // Minimum job number for failure
        int minFailureId = 2;

        public ComplexDependenciesParameters(String name, Long seed) {
            this.seed = seed;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public ComplexDependenciesParameters jobs(int nbJobs, int maxExcutionTime, int minExecutionTime) {
            this.nbJobs = nbJobs;
            this.maxExecutionTime = maxExcutionTime;
            this.minExecutionTime = minExecutionTime;
            return this;
        }

        public ComplexDependenciesParameters dependencies(double dependencyRatio, int maxDeps) {
            this.dependencyRatio = dependencyRatio;
            this.maxDeps = maxDeps;
            return this;
        }

        public ComplexDependenciesParameters failures(double failureRatio, int minFailures, int minFailureId) {
            this.failureRatio = failureRatio;
            this.minFailures = minFailures;
            this.minFailureId = minFailureId;
            return this;
        }
    }

    @DataProvider()
    static ComplexDependenciesParameters[][] complexDependenciesTestProvider() {
        return new ComplexDependenciesParameters[][]{
                {
                        new ComplexDependenciesParameters("complex", -8451050260222287949l)
                                .jobs(20, 50, 10)
                                .dependencies(.2, 200)
                                .failures(0.05, 2, 2)
                }
        };
    }

    @Test(description = "Run jobs generated at random", dataProvider = "complexDependenciesTestProvider")
    public void test_complex_dependencies(ComplexDependenciesParameters p) throws ExperimaestroCannotOverwrite {
        Random random = new Random();
        long seed = p.seed == null ? random.nextLong() : p.seed;
        LOGGER.info("Seed is %d", seed);
        random.setSeed(seed);

        int nbCouples = p.nbJobs * (p.nbJobs - 1) / 2;

        final int maxDependencies = min(p.maxDeps, nbCouples);

        File jobDirectory = mkTestDir();
        ThreadCount counter = new ThreadCount();

        WaitingJob[] jobs = new WaitingJob[p.nbJobs];

        // --- Generate the dependencies
        TreeSet<Link> dependencies = new TreeSet<>();

        int n = min(min((int) (long) (nbCouples * p.dependencyRatio * random.nextDouble()), Integer.MAX_VALUE), maxDependencies);

        long[] values = new long[n];
        RandomSampler.sample(n, nbCouples, n, 0, values, 0, random);
        LOGGER.debug("Sampling %d values from %d", n, nbCouples);
        for (long v : values) {
            final Link link = new Link(v);
            dependencies.add(link);
            LOGGER.debug("LINK %d to %d [%d]", link.from, link.to, v);
            assert link.from < p.nbJobs;
            assert link.to < p.nbJobs;
            assert link.from < link.to;
        }

        // --- Select the jobs that will fail
        ResourceState[] states = new ResourceState[jobs.length];
        for (int i = 0; i < states.length; i++)
            states[i] = ResourceState.DONE;
        n = (int) max(p.minFailures, random.nextDouble() * p.failureRatio * jobs.length);
        long[] values2 = new long[n];
        RandomSampler.sample(n, jobs.length - p.minFailureId, n, p.minFailureId, values2, 0, random);
        for (int i = 0; i < n; i++)
            states[((int) values2[i])] = ResourceState.ERROR;


        // --- Generate new jobs
        long duration = 0;
        int nbHolding = 0;
        for (int i = 0; i < jobs.length; i++) {

            int waitingTime = random.nextInt(p.maxExecutionTime - p.minExecutionTime) + p.minExecutionTime;
            jobs[i] = new WaitingJob(scheduler, counter, jobDirectory, "job" + i, waitingTime, states[i] == ResourceState.DONE ? 0 : 1);

            duration += waitingTime;

            ArrayList<String> deps = new ArrayList<>();
            for (Link link : dependencies.subSet(new Link(i, 0), true, new Link(i, Integer.MAX_VALUE), true)) {
                assert i == link.to;
                jobs[i].addDependency(jobs[link.from].createDependency(null));
                if (states[link.from].isBlocking())
                    states[i] = ResourceState.ON_HOLD;
                deps.add(jobs[link.from].toString());
            }

            if (states[i] == ResourceState.ON_HOLD) {
                nbHolding++;
            }

            scheduler.store(jobs[i], false);
            LOGGER.debug("Job [%s] created: final=%s, deps=%s", jobs[i], states[i], Output.toString(", ", deps));
        }

        LOGGER.info("Waiting for jobs to finish (%d remaining)", counter.getCount());
        // FIXME: revert back to this once bug is fixed
        while (counter.getCount() > 0) {
            counter.resume(0, p.maxExecutionTime + 5000, true);
            for (int i = 0; i < jobs.length; i++)
                if (jobs[i].getState().isActive())
                    LOGGER.warn("Job [%s] still active [%s]", jobs[i], jobs[i].getState());
        }
        int count = counter.getCount();

        LOGGER.info("Finished waiting [%d]: %d jobs remaining", System.currentTimeMillis(), counter.getCount());

        if (count > 0)
            LOGGER.error("Time out: %d jobs were not processed", count);

        // --- Check
        LOGGER.info("Checking job states");
        int errors = 0;
        for (int i = 0; i < jobs.length; i++)
            errors += checkState(EnumSet.of(states[i]), jobs[i]);

        LOGGER.info("Checking job dependencies");
        for (Link link : dependencies) {
            if (states[link.from] == ResourceState.DONE && jobs[link.to].getState() == ResourceState.DONE)
                errors += checkSequence(jobs[link.from], jobs[link.to]);
        }

        assert errors == 0 : "Detected " + errors + " errors after running jobs";
    }


    @Test(/*timeOut = 5000,*/ description = "Test of the token resource - one job at a time")
    public void test_token_resource() throws ExperimaestroCannotOverwrite, InterruptedException {

        File jobDirectory = mkTestDir();

        ThreadCount counter = new ThreadCount();
        ResourceLocator locator = new ResourceLocator(XPMConnector.getInstance(), "test");
        TokenResource token = new TokenResource(scheduler, new ResourceData(locator), 1);
        scheduler.store(token, false);

        WaitingJob[] jobs = new WaitingJob[5];
        for (int i = 0; i < jobs.length; i++) {
            jobs[i] = new WaitingJob(scheduler, counter, jobDirectory, "job" + i, 250, 0);
            jobs[i].addDependency(token.createDependency(null));
            scheduler.store(jobs[i], false);
        }


        counter.resume();

        // Check that one started after the other (since only one must have been active
        // at a time)
        LOGGER.info("Checking the token test output");
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
    static private int checkSequence(WaitingJob... jobs) {
        int errors = 0;

        for (int i = 0; i < jobs.length - 1; i++) {
            if (jobs[i].getEndTimestamp() >= jobs[i + 1].readyTimestamp) {
                LOGGER.warn("The jobs (%s/%x, end=%d) and (%s/%x, start=%d) did not start one after the other",
                        jobs[i], System.identityHashCode(jobs[i]), jobs[i].getEndTimestamp(),
                        jobs[i + 1], System.identityHashCode(jobs[i + 1]), jobs[i + 1].readyTimestamp);
                errors++;
            } else
                LOGGER.debug("The jobs (%s/%x) and (%s/%x) started one after the other [%dms]",
                        jobs[i], System.identityHashCode(jobs[i]), jobs[i + 1], System.identityHashCode(jobs[i + 1]), jobs[i + 1].readyTimestamp - jobs[i].getEndTimestamp());

        }
        return errors;
    }

    /**
     * Check the jobs states
     *
     * @param states The state
     * @param jobs   The jobs
     */
    private int checkState(EnumSet<ResourceState> states, WaitingJob... jobs) {
        int errors = 0;
        for (int i = 0; i < jobs.length; i++)
            if (!states.contains(jobs[i].getState())) {
                LOGGER.warn("The job (%s/%x) is not in one of the states %s but [%s]", jobs[i], System.identityHashCode(jobs[i]), states, jobs[i].getState());
                errors++;
            } else
                LOGGER.debug("The job (%s/%x) is in one of the states %s [%s]", jobs[i], System.identityHashCode(jobs[i]), states, jobs[i].getState());
        return errors;
    }


}
