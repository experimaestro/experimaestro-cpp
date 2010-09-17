package bpiwowar.expmanager.experiments;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

/**
 * This class generates new experiments
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Repository {
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
		experiments.put(information.id, information);
	}

	/**
	 * Register new experiment information
	 * 
	 * @param file
	 *            The file where the experimental information comes from
	 */
	public void register(File file) {
	}
}
