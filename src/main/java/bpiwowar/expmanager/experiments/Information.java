package bpiwowar.expmanager.experiments;

import static java.lang.String.format;

public abstract class Information {
	/**
	 * The identifier of this experiment 
	 */
	String id;
	
	/**
	 * Creates a new experiment
	 */
	abstract Experiment create();
	
	/**
	 * Documentation in XHTML format
	 */
	String getDocumentation() {
		return format("<p>No documentation found for experiment %s</p>", id);
	}
}
