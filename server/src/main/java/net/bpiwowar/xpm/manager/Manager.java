package net.bpiwowar.xpm.manager;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
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

import net.bpiwowar.xpm.utils.XMLUtils;
import org.w3c.dom.Element;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import static java.lang.String.format;

/**
 * Container for global definitions
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Manager {


    /**
     * Get the namespaces (default and element based)
     *
     * @param element
     */
    public static Map<String, String> getNamespaces(Element element) {
        TreeMap<String, String> map = new TreeMap<>();
        for (Entry<String, String> mapping : Constants.PREDEFINED_PREFIXES.entrySet())
            map.put(mapping.getKey(), mapping.getValue());
        for (Entry<String, String> mapping : XMLUtils.getNamespaces(element))
            map.put(mapping.getKey(), mapping.getValue());
        return map;
    }


}