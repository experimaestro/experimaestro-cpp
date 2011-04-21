package sf.net.experimaestro.manager;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

public class AlternativeType extends Type {
	private static final long serialVersionUID = 1L;

	/**
	 * The task factories that handles the values
	 */
	Map<QName, TaskFactory> factories = new HashMap<QName, TaskFactory>();

	/**
	 * Create a new type with alternatives
	 * @param qName
	 */
	public AlternativeType(QName qName) {
		super(qName);
	}

	/**
	 * Add a new factory
	 * @param name
	 * @param factory
	 */
	public void add(QName name, TaskFactory factory) {
		factories.put(name, factory);
	}


}
