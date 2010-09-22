package bpiwowar.expmanager.experiments;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class ExperimentJoin extends Experiment {
	Map<String, Experiment> experiments = new TreeMap<String, Experiment>();

	/**
	 * Add a new experiment
	 * 
	 * @param namespace
	 *            The namespace for this experiment
	 * @param experiment
	 */
	public void put(String namespace, Experiment experiment) {
		experiments.put(namespace, experiment);
	}

	@Override
	public void setParameter(QName id, Object value) {
		// Split

		// Find which experiments match
		if (id.size() >= 2) {
			Experiment experiment = experiments.get(id.get(0));
			if (experiment == null)
				throw new RuntimeException(format(
						"%s does not match any experiment", id.get(0)));
			experiment.setParameter(id.offset(1), value);
		} else {
			ArrayList<Entry<String, Experiment>> matches = new ArrayList<Entry<String, Experiment>>();

			for (Entry<String, Experiment> outer : experiments.entrySet()) {
				for (Entry<QName, Parameter> inner : outer.getValue()
						.getParameters().entrySet()) {
					if (inner.getKey().getName().equals(id.get(0)))
						matches.add(outer);
				}
			}
		}

	}

	@Override
	public Map<QName, Parameter> getParameters() {
		Map<QName, Parameter> map = new TreeMap<QName, Parameter>();
		for (Entry<String, Experiment> outer : experiments.entrySet()) {
			for (Entry<QName, Parameter> inner : outer.getValue()
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

		for (Entry<String, Experiment> outer : experiments.entrySet()) {
			for (Entry<QName, Type> inner : outer.getValue().getOutputs()
					.entrySet()) {
				map.put(new QName(outer.getKey(), inner.getKey()),
						inner.getValue());
			}
		}

		return map;
	}

	@Override
	Map<QName, Object> run() {
		// Run all the experiments and add the qualified name
		Map<QName, Object> map = new TreeMap<QName, Object>();
		for (Entry<String, Experiment> outer : experiments.entrySet()) {
			Map<QName, Object> outputs = outer.getValue().run();
			for (Entry<QName, Object> output : outputs.entrySet()) {
				map.put(new QName(outer.getKey(), output.getKey()),
						output.getValue());
			}
		}
		return map;

	}

}
