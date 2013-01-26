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
import com.sleepycat.je.EnvironmentLockedException;
import com.sleepycat.persist.model.Persistent;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.connectors.LocalhostConnector;
import sf.net.experimaestro.connectors.SingleHostConnector;
import sf.net.experimaestro.connectors.XPMProcess;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.locks.LockType;
import sf.net.experimaestro.utils.TemporaryDirectory;
import sf.net.experimaestro.utils.ThreadCount;
import sf.net.experimaestro.utils.log.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class SchedulerTest {

    final static private Logger LOGGER = Logger.getLogger();

    // To get the result of tasks
    static ArrayList<String> sequence = new ArrayList<String>();

    static class Counters {
        Map<String, ThreadCount> counters = new TreeMap<>();

        ThreadCount get(String id) {
            ThreadCount count = counters.get(id);
            if (id == null)
                counters.put(id, count = new ThreadCount());
            return count;
        }

        public void add(String id) {
            if (id == null)
                return;
            get(id).add();
        }

        public void resume(String id) {
            if (id == null)
                return;
            get(id).resume();
        }

    }

    static Counters counters = new Counters();

    @Persistent
    static public class SimpleJob extends Job {
        private String waitId;
        private String id;
        private ThreadCount counter;

        public SimpleJob() {
        }

        public SimpleJob(String id, Scheduler scheduler, Connector connector, String fullId, String waitId,
                         String setId) {
            super(scheduler, connector, fullId);
            this.id = id;
            this.waitId = waitId;
            this.state = ResourceState.WAITING;
            counter.add(1);
            counters.add(setId);
        }

        @Override
        public synchronized void notify(Resource resource, Object... objects) {
            super.notify(resource, objects);
            if (resource == null) {

            }
        }

        @Override
        protected XPMProcess startJob(ArrayList<Lock> locks) throws Throwable {
            // Wait that this task has been added to the queue
            counters.resume(waitId);

            synchronized (sequence) {
                sequence.add(id);
            }
            counter.del();
            return null;
        }
    }


    TemporaryDirectory directory;
    Scheduler scheduler;

    @BeforeClass
    public void init() throws IOException {
        directory = new TemporaryDirectory("scheduler-tests", "dir");
        final File dbFile = new File(directory.getFile(), "db");
        dbFile.mkdir();
        scheduler = new Scheduler(dbFile);
    }

    @AfterClass
    public void close() {
        scheduler.close();
    }


    @Test(timeOut = 1000, description = "Run two jobs - one depend on the other to start", enabled = false)
    public void test_simple_dependency() throws EnvironmentLockedException,
            DatabaseException, IOException, InterruptedException {
        File jobDirectory = new File(directory.getFile(), "simple");

        // Create two jobs: job1, and job2 that depends on job1
        SimpleJob job1 = simpleJob(scheduler, jobDirectory, "job1", false,
                "job2");
        SimpleJob job2 = simpleJob(scheduler, jobDirectory, "job2", true,
                null);
        job2.addDependency(job1, LockType.READ_ACCESS);

        scheduler.store(null, job1, null);
        scheduler.store(null, job2, null);
//        counter.resume();

        assert sequence.get(0).equals("job1");
        assert sequence.get(1).equals("job2");

    }

    @Test(timeOut = 5000, description = "Test of the token resource")
    public void test_token_resource() {
        File jobDirectory = new File(directory.getFile(), "token");
        jobDirectory.mkdirs();

        ThreadCount counter = new ThreadCount();
        TokenResource resource = new TokenResource(scheduler, "test", 1);
        scheduler.store(resource, null);

        WaitingJob[] jobs = new WaitingJob[2];
        for(int i = 0; i < jobs.length; i++) {
            jobs[i] = new WaitingJob(scheduler, counter, jobDirectory,  "job" + i, 500);
            jobs[i].addDependency(resource, LockType.READ_ACCESS);
            scheduler.store(jobs[i], null);
        }


        counter.resume();

        Arrays.sort(jobs, new Comparator<WaitingJob>() {
            @Override
            public int compare(WaitingJob o1, WaitingJob o2) {
                return Long.compare(o1.getStartTimestamp(), o2.getStartTimestamp());
            }
        });

        // Check that one started after the other
        for(int i = 0; i < jobs.length - 1; i++)
            assert jobs[i].getEndTimestamp() < jobs[i+1].getStartTimestamp()
                : "The jobs did not start one after the other";

        LOGGER.info("Finished token test");
    }



    static SimpleJob simpleJob(Scheduler scheduler, File jobDirectory,
                               final String id, boolean set, String waitId)
            throws DatabaseException {
        final SimpleJob job = new SimpleJob(id, scheduler, LocalhostConnector.getInstance(), new File(jobDirectory,
                id).getAbsolutePath(), set ? id : null, waitId);

        return job;
    }

    @Test(timeOut = 5000, description = "Check that when a resource [1] depends on [2], and [2] gets an error, [1] is put on hold," +
            "and that [1] put in ready makes [2] waiting")
    public void test_error_state() {

    }

    @Persistent
    static private class WaitingJob extends Job {
        transient private ThreadCount counter;
        long duration;

        public WaitingJob() {}
        public WaitingJob(Scheduler scheduler, ThreadCount counter, File dir, String id, long duration) {
            super(scheduler, LocalhostConnector.getInstance(), new File(dir, id).getAbsolutePath());
            this.counter = counter;
            counter.add();
            state = ResourceState.READY;
        }

        @Override
        protected XPMProcess startJob(ArrayList<Lock> locks) throws Throwable {
            return new MyXPMProcess(counter, getMainConnector(), this, duration);
        }

        @Persistent
        static private class MyXPMProcess extends XPMProcess {
            private long timestamp;
            private long duration;
            private transient ThreadCount counter;

            public MyXPMProcess() {

            }
            public MyXPMProcess(ThreadCount counter, SingleHostConnector connector, Job job, long duration) {
                super(connector, "1", job, true);
                this.timestamp = System.currentTimeMillis();
                this.duration = duration;
                this.counter = counter;
            }

            @Override
            public void dispose() {
                counter.del();
                super.dispose();
            }

            @Override
            public int waitFor() throws InterruptedException {
                synchronized (this) {
                    long toWait = duration - (System.currentTimeMillis() - timestamp);
                    if (toWait > 0) wait(toWait);
                }
                return 0;
            }

            @Override
            public boolean isRunning() throws Exception {
                return  duration < System.currentTimeMillis() - timestamp;
            }

            @Override
            public int exitValue() {
                return 0;
            }

            @Override
            public OutputStream getOutputStream() {
                return null;
            }

            @Override
            public InputStream getErrorStream() {
                return null;
            }

            @Override
            public InputStream getInputStream() {
                return null;
            }

            @Override
            public void destroy() {
            }
        }
    }
}
