package sf.net.experimaestro.manager;

import java.util.Map;
import java.util.TreeMap;

/**
 * Container for global definitions
 *  
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Manager {
	public static final String EXPERIMAESTRO_NS = "http://experimaestro.sf.net";
	public static final Map<String, String> PREDEFINED_PREFIXES = new TreeMap<String, String>();
	static {
		PREDEFINED_PREFIXES.put("xp", EXPERIMAESTRO_NS);
		PREDEFINED_PREFIXES.put("xs", "http://www.w3.org/2001/XMLSchema");
	}

}
