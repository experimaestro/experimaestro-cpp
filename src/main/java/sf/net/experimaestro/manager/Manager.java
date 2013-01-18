/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
 *
 * experimaestro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * experimaestro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 */

package sf.net.experimaestro.manager;

import org.w3c.dom.Element;
import sf.net.experimaestro.utils.XMLUtils;

import javax.xml.xquery.XQException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Container for global definitions
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Manager {

	public static final String EXPERIMAESTRO_NS = "http://experimaestro.lip6.fr";
	public static final Map<String, String> PREDEFINED_PREFIXES = new TreeMap<>();
    public static final String EXPERIMAESTRO_PREFIX = "xp";

    public static final String XMLSCHEMA_NS = "http://www.w3.org/2001/XMLSchema";
    public static final QName XP_PATH = new QName(EXPERIMAESTRO_NS, "path");

    static {
		PREDEFINED_PREFIXES.put("xp", EXPERIMAESTRO_NS);
		PREDEFINED_PREFIXES.put("xs", XMLSCHEMA_NS);
	}

	/**
	 * Get the namespaces (default and element based)
	 * 
	 * @param element
	 * @throws XQException
	 */
	public static Map<String, String> getNamespaces(Element element) {
		TreeMap<String, String> map = new TreeMap<>();
		for (Entry<String, String> mapping : PREDEFINED_PREFIXES.entrySet())
			map.put(mapping.getKey(), mapping.getValue());
		for (Entry<String, String> mapping : XMLUtils.getNamespaces(element))
			map.put(mapping.getKey(), mapping.getValue());
		return map;
	}

}
