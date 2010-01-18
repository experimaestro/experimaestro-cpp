package bpiwowar.expmanager;

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
	 * The job that can generate this data
	 */
	transient Job generatingJob;

	public boolean isProduced() {
		return produced;
	}
}
