package sf.net.experimaestro.scheduler;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.EnvironmentLockedException;
import com.sleepycat.persist.model.Persistent;
import org.testng.annotations.Test;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.locks.LockType;
import sf.net.experimaestro.utils.TemporaryDirectory;
import sf.net.experimaestro.utils.ThreadCount;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class SchedulerTest {

	static ThreadCount counter = new ThreadCount();

	// To get the result of tasks
	static ArrayList<String> sequence = new ArrayList<String>();

	static class Counters {
		Map<String, ThreadCount> counters = new TreeMap<String, ThreadCount>();

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

		public SimpleJob() {
		}

		public SimpleJob(String id, Scheduler scheduler, String fullId, String waitId,
				String setId) {
			super(scheduler, fullId);
			this.id = id;
			this.waitId = waitId;
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
		protected int doRun(ArrayList<Lock> locks) throws Throwable {
			// Wait that this task has been added to the queue
			counters.resume(waitId);
			
			synchronized (sequence) {
				sequence.add(id);
			}
			counter.del();
			return 0;
		}
	}

	@Test(timeOut = 1000, description = "Run two jobs - one depend on the other to start")
	public void test_simple_dependency() throws EnvironmentLockedException,
			DatabaseException, IOException, InterruptedException {
		TemporaryDirectory directory = null;
		try {
			directory = new TemporaryDirectory("scheduler-tests", "dir");

            final File dbFile = new File(directory.getFile(),
                    "db");
            dbFile.mkdir();
            Scheduler scheduler = new Scheduler(dbFile, 5);
			File jobDirectory = new File(directory.getFile(), "jobs");
			
			// Create two jobs: job1, and job2 that depends on job1
			SimpleJob job1 = simpleJob(scheduler, jobDirectory, "job1", false,
					"job2");
			SimpleJob job2 = simpleJob(scheduler, jobDirectory, "job2", true,
					null);
			job2.addDependency(job1, LockType.READ_ACCESS);
			
			scheduler.add(job1, job2);
			counter.resume();

			assert sequence.get(0).equals("job1");
			assert sequence.get(1).equals("job2");
			
			scheduler.close();
		} finally {
			if (directory != null)
				directory.close();
		}
	}

	static SimpleJob simpleJob(Scheduler scheduler, File jobDirectory,
			final String id, boolean set, String waitId)
			throws DatabaseException {
		final SimpleJob job = new SimpleJob(id, scheduler, new File(jobDirectory,
				id).getAbsolutePath(), set ? id : null, waitId);
		return job;
	}
}
