package sf.net.experimaestro.scheduler;

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

import bpiwowar.argparser.utils.Output;
import org.apache.commons.lang.mutable.MutableLong;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import sf.net.experimaestro.connectors.NetworkShare;
import sf.net.experimaestro.exceptions.ExperimaestroCannotOverwrite;
import sf.net.experimaestro.utils.RandomSampler;
import sf.net.experimaestro.utils.ThreadCount;
import sf.net.experimaestro.utils.XPMEnvironment;
import sf.net.experimaestro.utils.log.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.Random;
import java.util.TreeSet;

import static java.lang.Math.*;
import static java.lang.String.format;
import static sf.net.experimaestro.scheduler.WaitingJobProcess.Action;

public class SchedulerTest extends XPMEnvironment {
    // Time to  process a job
    static final long JOB_PROCESSING_TIME = 50;

    final static private Logger LOGGER = Logger.getLogger();

    @BeforeSuite
    public static void setup() throws Throwable {
        prepare();
    }

    @Test
    public void test_basic() throws ExperimaestroCannotOverwrite, IOException, SQLException {
        new ComplexDependenciesParameters("basic", 132481234l)
                .jobs(10, 50, 5)
                .dependencies(.5, 2)
                .failures(0, 0, 0)
                .token(0)
                .run();
    }

    @Test
    public void test_failures() throws ExperimaestroCannotOverwrite, IOException, SQLException {
        new ComplexDependenciesParameters("failures", 132481234l)
                .jobs(10, 50, 10)
                .dependencies(.5, 2)
                .failures(0.10, 1, 0)
                .token(0)
                .run();
    }

    @Test
    public void test_failures_and_tokens() throws ExperimaestroCannotOverwrite, IOException, SQLException {
        new ComplexDependenciesParameters("failures-and-tokens", -8451050260222287949l)
                .jobs(100, 150, 10)
                .dependencies(.2, 25)
                .failures(0.10, 3, 2)
                .token(3)
                .run();
    }

    /**
     * Check that these runners started one after the other
     *
     * @param runners   The runners status check
     * @param readyness true if a job must finish before the next is ready
     * @param reorder   true if jobs can be run in any order, but one at a time
     */
    static private int checkSequence(boolean reorder, boolean readyness, WaitingJob... runners) throws SQLException {
        int errors = 0;
        Job[] jobs = new Job[runners.length];

        for (int i = 0; i < runners.length; i++) {
            jobs[i] = (Job) Resource.getById(runners[i].getId());
        }

        if (reorder) {
            Arrays.sort(jobs, (o1, o2) -> Long.compare(o1.getStartTimestamp(), o2.getStartTimestamp()));
        }

        for (int i = 0; i < jobs.length - 1; i++) {
            final long nextTimestamp = readyness ? ((WaitingJob) jobs[i + 1]).status().readyTimestamp : jobs[i + 1].getStartTimestamp();
            final long endTimestamp = jobs[i].getEndTimestamp();

            if (endTimestamp > nextTimestamp) {
                LOGGER.warn("The runners (%s/%x, end=%d) and (%s/%x, start=%d) did not start one after the other",
                        jobs[i], System.identityHashCode(jobs[i]), endTimestamp,
                        jobs[i + 1], System.identityHashCode(jobs[i + 1]), nextTimestamp);
                errors++;
            } else
                LOGGER.debug("The runners (%s/%x) and (%s/%x) started one after the other [%dms]",
                        jobs[i], System.identityHashCode(jobs[i]), jobs[i + 1],
                        System.identityHashCode(jobs[i + 1]),
                        nextTimestamp - endTimestamp);

        }

        return errors;
    }

    @Test(description = "Run two jobs - one depend on the other status start")
    public void test_simple_dependency() throws
            IOException, InterruptedException, ExperimaestroCannotOverwrite, SQLException {

        File jobDirectory = mkTestDir();
        ThreadCount counter = new ThreadCount();

        // Create two jobs: job1, and job2 that depends on job1
        WaitingJob[] jobs = new WaitingJob[2];
        for (int i = 0; i < jobs.length; i++) {
            final int finalI = i;
            jobs[finalI] = new WaitingJob(counter, jobDirectory, "job" + finalI, new Action(500, 0, 0));
            if (finalI > 0) {
                jobs[finalI].addDependency(jobs[finalI - 1].createDependency());
            }
            jobs[finalI].save();
        }


        int errors = 0;
        waitToFinish(0, counter, jobs, 2500, 5);

        errors += checkSequence(false, true, jobs);
        errors += checkState(EnumSet.of(ResourceState.DONE), jobs);
        Assert.assertTrue(errors == 0, "Detected " + errors + " errors after running jobs");

    }

    @Test(description = "Run two jobs - one depend on the other status start, the first fails")
    public void test_failed_dependency() throws
            IOException, InterruptedException, ExperimaestroCannotOverwrite, SQLException {

        File jobDirectory = mkTestDir();
        ThreadCount counter = new ThreadCount();

        // Create two jobs: job1, and job2 that depends on job1
        WaitingJob[] jobs = new WaitingJob[2];
        for (int i = 0; i < jobs.length; i++) {
            final int finalI = i;

            jobs[finalI] = new WaitingJob(counter, jobDirectory, "job" + finalI, new Action(500, finalI == 0 ? 1 : 0, 0));
            if (finalI > 0) {
                jobs[finalI].addDependency(jobs[finalI - 1].createDependency());
            }
            jobs[finalI].save();
        }


        waitToFinish(0, counter, jobs, 1500, 5);

        int errors = 0;
        errors += checkState(EnumSet.of(ResourceState.ERROR), jobs[0]);
        errors += checkState(EnumSet.of(ResourceState.ON_HOLD), jobs[1]);
        Assert.assertTrue(errors == 0, "Detected " + errors + " errors after running jobs");
    }


    private void waitToFinish(int limit, ThreadCount counter, WaitingJob[] jobs, long timeout, int tries) {
        LOGGER.info("Waiting for operations status finish [limit=%d, tries=%d]", limit, tries);
        int loop = 0;
        while (counter.getCount() > limit && loop++ < tries) {
            counter.resume(limit, timeout, true);
            int count = counter.getCount();
            if (count <= limit) {
                LOGGER.info("OK - counter %d is below the limit %d", count, limit);
                break;
            }

            LOGGER.info("Waiting status finish - %d active jobs > %d [%d]", count, limit, loop);
            for (int i = 0; i < jobs.length; i++) {
                if (jobs[i].getState().isActive()) {
                    LOGGER.warn("Job [%s] still active [%s]", jobs[i], jobs[i].getState());
                }
            }
        }

        Assert.assertTrue(counter.getCount() <= limit, "Too many uncompleted jobs");
    }

    private void waitBeforeCheck() {
        synchronized (this) {
            try {
                wait(1000);
            } catch (InterruptedException e) {
                LOGGER.error(e, "error while waiting");
            }
        }
    }

    @Test(description = "The required dependency cannot be deleted")
    public void test_cannot_delete_required() throws IOException, SQLException {
        File jobDirectory = mkTestDir();
        ThreadCount counter = new ThreadCount();


        WaitingJob jobA = new WaitingJob(counter, jobDirectory, "jobA", new Action(250, 0, 0));
        jobA.save();
        WaitingJob jobB = new WaitingJob(counter, jobDirectory, "jobB", new Action(250, 0, 0));
        jobB.addDependency(jobA.createDependency());
        jobB.save();

        waitToFinish(0, counter, new WaitingJob[] { jobA, jobB }, 1000, 3);

        try {
            jobA.delete(false);
            Scheduler.get().resources().delete(jobA);
            throw new AssertionError("Deletion of requirement should have been prevented");
        } catch(RuntimeException e) {
        }

        jobB.delete(false);
        jobA.delete(false);
    }


    @Test(description = "The required dependency ends before the new job is saved")
    public void test_required_job_ends() throws IOException, SQLException {
        File jobDirectory = mkTestDir();
        ThreadCount counter = new ThreadCount();

        final int lockA = IntLocks.newLockID();
        WaitingJob jobA = new WaitingJob(counter, jobDirectory, "jobA",
                new Action(250, 0, 0).removeLock(lockA));
        jobA.save();

        WaitingJob jobB = new WaitingJob(counter, jobDirectory, "jobB", new Action(250, 0, 0));
        jobB.addDependency(jobA.createDependency());

        // Wait that A ends
        IntLocks.waitLockID(lockA);
        jobB.save();

        LOGGER.info("FINISHED LAUNCHING B");


        waitToFinish(0, counter, new WaitingJob[]{
                jobA, jobB
        }, 1500, 5);

    }

    @Test(description = "Test of the token resource - one job at a time")
    public void test_token_resource() throws ExperimaestroCannotOverwrite, InterruptedException, IOException, SQLException {

        File jobDirectory = mkTestDir();

        ThreadCount counter = new ThreadCount();
        TokenResource token = new TokenResource(NetworkShare.uriToPath("/tokens/scheduler_test/test_token_resource"), 1);
        token.save();

        // Sets 5 jobs
        WaitingJob[] jobs = new WaitingJob[5];
        BitSet failure = new BitSet();
        failure.set(3);

        for (int i = 0; i < jobs.length; i++) {
            jobs[i] = new WaitingJob(counter, jobDirectory, "job" + i, new Action(250, failure.get(i) ? 1 : 0, 0));
            final WaitingJob job = jobs[i];
            job.addDependency(token.createDependency());
            job.save();

        }


        waitToFinish(0, counter, jobs, 1500, 5);
        waitBeforeCheck();

        // Check that one started after the other (since only one must have been active
        // at a time)
        LOGGER.info("Checking the token test output");

        // Retrieve all the jobs

        int errors = 0;
        errors += checkSequence(true, false, jobs);
        for (int i = 0; i < jobs.length; i++) {
            errors += checkState(jobs[i].finalCode() != 0 ? EnumSet.of(ResourceState.ERROR) : EnumSet.of(ResourceState.DONE), jobs[i]);
        }
        Assert.assertTrue(errors == 0, "Detected " + errors + " errors after running jobs");
    }

    @Test(description = "If all failed dependencies are restarted, a job should get back status a WAITING state")
    public void test_hold_dependencies() throws Exception {
        WaitingJob[] jobs = new WaitingJob[3];
        ThreadCount counter = new ThreadCount();
        File jobDirectory = mkTestDir();

        // We create 3 jobs - and make the first fail
        for (int i = 0; i < jobs.length; i++) {
            jobs[i] = new WaitingJob(counter, jobDirectory, "job" + i, new Action(500, i == 0 ? 1 : 0, 0));
            final int finalI = i;
            if (finalI > 0) {
                jobs[finalI].addDependency(jobs[finalI - 1].createDependency());
            }
            WaitingJob job = jobs[finalI];
            job.save();
        }

        // Wait
        LOGGER.info("Waiting for job 0 status fail");
        int errors = 0;
        waitToFinish(0, counter, jobs, 1500, 5);
        errors += checkState(EnumSet.of(ResourceState.ERROR), jobs[0]);
        errors += checkState(EnumSet.of(ResourceState.ON_HOLD), jobs[1], jobs[2]);

        // We wait until the two jobs are stopped
        LOGGER.info("Restarting job 0 with happy ending");
        jobs[0].restart(new Action(500, 0, 0));
        waitToFinish(0, counter, jobs, 1500, 5);
        errors += checkState(EnumSet.of(ResourceState.DONE), jobs);
        Assert.assertTrue(errors == 0, "Detected " + errors + " errors after running jobs");
    }

    /**
     * Check the jobs states
     *
     * @param states The state
     * @param jobs   The jobs
     */
    private int checkState(EnumSet<ResourceState> states, WaitingJob... jobs) throws SQLException {

        int errors = 0;
        for (int i = 0; i < jobs.length; i++) {
            Job job = (Job) Resource.getById(jobs[i].getId());
            if (!states.contains(job.getState())) {
                LOGGER.warn("The job (%s) is not in one of the states %s but [%s]", jobs[i], states, job.getState());
                errors++;
            } else
                LOGGER.debug("The job (%s) is in one of the states %s [%s]", jobs[i], states, job.getState());
        }

        return errors;

    }


// ----- Utility methods for scheduler

    final static public class Link implements Comparable<Link> {
        int to, from;

        public Link(int to, int from) {
            this.to = to;
            this.from = from;
        }

        /**
         * Initialize a link between two jobs from a single number
         * <p>
         * The association is as follows 1 = 1-2, 2 = 1-3, 3 = 2-3, 4 = 1-4, ...
         *
         * @param n is
         */
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
            return from == link.from;

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

    public class ComplexDependenciesParameters {
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

        // Tokens
        int token = 0;

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

        public ComplexDependenciesParameters token(int token) {
            this.token = token;
            return this;
        }


        public void run() throws ExperimaestroCannotOverwrite, IOException, SQLException {
            Random random = new Random();
            long seed = this.seed == null ? random.nextLong() : this.seed;
            LOGGER.info("Seed is %d", seed);
            random.setSeed(seed);

            // Prepares directory and counter
            File jobDirectory = new File(mkTestDir(), name);
            jobDirectory.mkdir();
            ThreadCount counter = new ThreadCount();

            // Our set of jobs
            WaitingJob[] jobs = new WaitingJob[nbJobs];

            // --- Generate the dependencies

            // Number of potential dependencies
            int nbCouples = nbJobs * (nbJobs - 1) / 2;

            // Maximum number of dependencies
            final int maxDependencies = min(maxDeps, nbCouples);

            // The list of dependencies
            TreeSet<Link> dependencies = new TreeSet<>();

            // Number of generated dependencies
            int n = min(min((int) (long) (nbCouples * dependencyRatio * random.nextDouble()), Integer.MAX_VALUE), maxDependencies);
            long[] values = new long[n];
            // Draw n dependencies among nbCouples possible
            RandomSampler.sample(n, nbCouples, n, 0, values, 0, random);

            LOGGER.debug("Sampling %d values from %d", n, nbCouples);
            for (long v : values) {
                final Link link = new Link(v);
                dependencies.add(link);
                LOGGER.debug("LINK %d status %d [%d]", link.from, link.to, v);
                assert link.from < nbJobs;
                assert link.to < nbJobs;
                assert link.from < link.to;
            }

            // --- Select the jobs that will fail
            ResourceState[] states = new ResourceState[jobs.length];
            for (int i = 0; i < states.length; i++)
                states[i] = ResourceState.DONE;
            n = (int) max(minFailures, random.nextDouble() * failureRatio * jobs.length);
            long[] values2 = new long[n];
            RandomSampler.sample(n, jobs.length - minFailureId, n, minFailureId, values2, 0, random);
            for (int i = 0; i < n; i++)
                states[((int) values2[i])] = ResourceState.ERROR;

            // --- Generate token resource
            final TokenResource token;
            if (this.token > 0) {
                final Path path = NetworkShare.uriToPath(format("/scheduler_test/test_complex_dependency/%s", name));
                token = new TokenResource(path, this.token);
                token.save();
            } else {
                token = null;
            }

            final MutableLong totalTime = new MutableLong();

            // --- Generate new jobs
            for (int i = 0; i < jobs.length; i++) {
                final int j = i;

                int waitingTime = random.nextInt(maxExecutionTime - minExecutionTime) + minExecutionTime;
                jobs[j] = new WaitingJob(counter, jobDirectory, "job" + j, new Action(waitingTime, states[j] == ResourceState.DONE ? 0 : 1, 0));

                totalTime.add(jobs[j].totalTime() + JOB_PROCESSING_TIME);

                ArrayList<String> deps = new ArrayList<>();
                for (Link link : dependencies.subSet(new Link(j, 0), true, new Link(j, Integer.MAX_VALUE), true)) {
                    assert j == link.to;
                    jobs[j].addDependency(jobs[link.from].createDependency());
                    if (states[link.from].isBlocking())
                        states[j] = ResourceState.ON_HOLD;
                    deps.add(jobs[link.from].toString());
                }

                if (token != null) {
                    jobs[j].addDependency(token.createDependency());
                }

                try {
                    jobs[j].save();
                } catch (SQLException e) {
                    LOGGER.error(e, "Error while saving job %d: path=%s", j, jobs[j].getLocator());
                }
                LOGGER.debug("Job [%s] created: final=%s, deps=%s", jobs[j], states[j], Output.toString(", ", deps));
            }


            LOGGER.info("Waiting for jobs status finish (%d remaining) / total time = %dms",
                    counter.getCount(), totalTime.longValue());

            waitToFinish(0, counter, jobs, totalTime.longValue(), 5);

            waitBeforeCheck();

            int count = counter.getCount();

            LOGGER.info("Finished waiting [%d]: %d jobs remaining", System.currentTimeMillis(), counter.getCount());

            if (count > 0) {
                LOGGER.error("Time out: %d jobs were not processed", count);
            }

            // --- Check
            LOGGER.info("Checking job states");
            int errors = 0;
            for (int i = 0; i < jobs.length; i++)
                errors += checkState(EnumSet.of(states[i]), jobs[i]);

            LOGGER.info("Checking job dependencies");
            for (Link link : dependencies) {
                if (states[link.from] == ResourceState.DONE && jobs[link.to].getState() == ResourceState.DONE)
                    errors += checkSequence(false, true, jobs[link.from], jobs[link.to]);
            }

            Assert.assertTrue(errors == 0, "Detected " + errors + " errors after running jobs");
        }

    }


}
