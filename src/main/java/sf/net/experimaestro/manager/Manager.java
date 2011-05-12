package sf.net.experimaestro.manager;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.xml.xquery.XQException;

import org.w3c.dom.Element;

import sf.net.experimaestro.utils.XMLUtils;

/**
 * Container for global definitions
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Manager {

	public static final String EXPERIMAESTRO_NS = "http://experimaestro.sf.net";
	public static final Map<String, String> PREDEFINED_PREFIXES = new TreeMap<String, String>();
	public static final String EXPERIMAESTRO_PREFIX = "xp";
	static {
		PREDEFINED_PREFIXES.put("xp", EXPERIMAESTRO_NS);
		PREDEFINED_PREFIXES.put("xs", "http://www.w3.org/2001/XMLSchema");
	}

	/**
	 * Get the namespaces (default and element based)
	 * 
	 * @param xqsc
	 * @param element
	 * @throws XQException
	 */
	public static Map<String, String> getNamespaces(Element element) {
		TreeMap<String, String> map = new TreeMap<String, String>();
		for (Entry<String, String> mapping : PREDEFINED_PREFIXES.entrySet())
			map.put(mapping.getKey(), mapping.getValue());
		for (Entry<String, String> mapping : XMLUtils.getNamespaces(element))
			map.put(mapping.getKey(), mapping.getValue());
		return map;
	}

}
