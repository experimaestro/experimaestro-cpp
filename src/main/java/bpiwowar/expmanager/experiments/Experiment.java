package bpiwowar.expmanager.experiments;

import java.util.List;
import java.util.Map;

public abstract class Experiment {
	Information information;

	/**
	 * Set a parameter
	 * 
	 * @param id
	 * @param value
	 */
	abstract public void setParameter(String id, Variable value);

	/**
	 * Get the list of parameters
	 */
	abstract public Map<String, Parameter> getParameters();

	/**
	 * Get the current outputs
	 */
	abstract public Map<String, Type> getOutputs();

	/**
	 * Run this experiment
	 * 
	 * @return The outputs
	 */
	abstract Map<String, Variable> run();

}
