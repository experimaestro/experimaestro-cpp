package bpiwowar.expmanager.experiments;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

/**
 * Meta-task composed of several interconnected tasks
 *  
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class TaskJoin extends Task {
	/**
	 * Maps a local identifier to an experiment
	 */
	Map<String, Task> experiments = new TreeMap<String, Task>();

	/**
	 * Add a new experiment
	 * 
	 * @param namespace
	 *            The namespace for this experiment
	 * @param experiment
	 */
	public void put(String namespace, Task experiment) {
		experiments.put(namespace, experiment);
	}

	@Override
	public void setParameter(QName id, Object value) {
		// Split

		// Find which experiments match
		if (id.size() >= 2) {
			Task experiment = experiments.get(id.get(0));
			if (experiment == null)
				throw new RuntimeException(format(
						"%s does not match any experiment", id.get(0)));
			experiment.setParameter(id.offset(1), value);
		} else {
			ArrayList<Entry<String, Task>> matches = new ArrayList<Entry<String, Task>>();

			for (Entry<String, Task> outer : experiments.entrySet()) {
				for (Entry<QName, NamedParameter> inner : outer.getValue()
						.getParameters().entrySet()) {
					if (inner.getKey().getName().equals(id.get(0)))
						matches.add(outer);
				}
			}
		}

	}

	@Override
	public Map<QName, NamedParameter> getParameters() {
		Map<QName, NamedParameter> map = new TreeMap<QName, NamedParameter>();
		for (Entry<String, Task> outer : experiments.entrySet()) {
			for (Entry<QName, NamedParameter> inner : outer.getValue()
					.getParameters().entrySet()) {
				map.put(new QName(outer.getKey(), inner.getKey()),
						inner.getValue());
			}
		}

		return map;
	}

	@Override
	public Map<QName, Type> getOutputs() {
		Map<QName, Type> map = new TreeMap<QName, Type>();

		for (Entry<String, Task> outer : experiments.entrySet()) {
			for (Entry<QName, Type> inner : outer.getValue().getOutputs()
					.entrySet()) {
				map.put(new QName(outer.getKey(), inner.getKey()),
						inner.getValue());
			}
		}

		return map;
	}

	@Override
	Document run() {
		return null;
	}

}
