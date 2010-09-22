package bpiwowar.expmanager.experiments;

import java.util.Map;

public abstract class Experiment {
	/**
	 * The information related to this class of experiment
	 */
	Information information;

	/**
	 * Set a parameter
	 * 
	 * @param id
	 *            The identifier for this parameter
	 * @param value
	 *            The value to be set
	 */
	abstract public void setParameter(QName id, Object value);

	/**
	 * Get the list of parameters
	 */
	abstract public Map<QName, Parameter> getParameters();

	/**
	 * Get the current outputs (given the current parameters)
	 */
	abstract public Map<QName, Type> getOutputs();

	/**
	 * Run this experiment. The outputs can be of predefined types (
	 * {@linkplain Integer}, {@linkplain Double}, etc. )
	 * 
	 * @return The outputs
	 */
	abstract Map<QName, Object> run();

}
