package bpiwowar.expmanager.jobs;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import bpiwowar.log.Logger;
import bpiwowar.utils.Heap;

/**
 * Thread manager for running commands - it has a pool of runs
 * 
 * @author bpiwowar
 */
public class JobManager {
	final static private Logger logger = Logger.getLogger();

	/**
	 * Number of threads
	 */
	int nbThreads = 5;

	/**
	 * The list of jobs
	 */
	Heap<Job> jobs;

	/**
	 * Our pool for threads
	 */
	private ExecutorService pool;

	/**
	 * Initialise a new job manager
	 * 
	 * @param nbThreads
	 */
	public JobManager(int nbThreads) {
		this.nbThreads = nbThreads;
		pool = Executors.newFixedThreadPool(nbThreads);
	}

	/**
	 */
	public void execute(Job job) {
		logger.info("Executing %s", job);
		jobs.add(job);
	}
}
