package bpiwowar.expmanager.experiments;

import java.util.Map;
import java.util.TreeMap;

import bpiwowar.log.Logger;

/**
 * This class generates new experiments
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Repository {
	final static private Logger LOGGER = Logger.getLogger();
	
	/**
	 * The list of available experiments
	 */
	Map<String, Information> experiments = new TreeMap<String, Information>();
	
	/**
	 * @return
	 */
	public Iterable<Information> experiments() {
		return experiments.values();
	}

	/**
	 * Return information about an experiment
	 * 
	 * @param name
	 * @return
	 */
	public Information get(String name) {
		return experiments.get(name);
	}

	/**
	 * Register new experiment information
	 * 
	 * @param information
	 */
	public void register(Information information) {
		LOGGER.info("Registering experiment %s", information.id);
		experiments.put(information.id, information);
	}

}
