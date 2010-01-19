package bpiwowar.expmanager;

import java.util.HashSet;
import java.util.Set;

import bpiwowar.expmanager.jobs.Job;

import com.sleepycat.persist.model.PrimaryKey;

/**
 * Represents some data that can be produced by a given job
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Data {
	/**
	 * The signature uniquely identifies a piece of data
	 */
	@PrimaryKey
	String signature;
	
	/**
	 * True when the data has been produced 
	 */
	boolean produced = false;
	
	/** 
	 * The job that can will or has generated this data
	 */
	transient Job generatingJob;

	public boolean isProduced() {
		return produced;
	}

	
	transient Set<Job> listeners = new HashSet<Job>();
	
	public void register(Job job) {
		listeners.add(job);		
	}

	public void unregister(Job job) {
		listeners.remove(job);
	}
}
