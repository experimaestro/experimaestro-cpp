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

import javax.xml.xquery.XQException;
import javax.xml.xquery.XQStaticContext;
import java.util.HashMap;
import java.util.Map;

/**
 * Defines a connection to between one or more output values and one input
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public abstract class Connection {
    private Map<String, String> namespaces = new HashMap<>();

    /** The destination */
    final DotName to;

    /** Required flag */
    boolean required;

    public Connection( DotName to) {
        this.to = to;
    }

    public void addNamespaces(Element element) {
        namespaces.putAll(Manager.getNamespaces(element));
    }

    /**
     * Set the defined namespaces during XQuery
     * @param xqsc
     * @throws XQException
     */
    public void setNamespaces(XQStaticContext xqsc) throws XQException {
        for (Map.Entry<String, String> mapping : namespaces.entrySet()) {
            Input.LOGGER.debug("Setting default namespace mapping [%s] to [%s]",
                    mapping.getKey(), mapping.getValue());
            xqsc.declareNamespace(mapping.getKey(), mapping.getValue());
        }
    }

    /**
     * Get the mapping between variable names and inputs
     * @return An iterable object
     */
    abstract public Iterable<? extends Map.Entry<String, DotName>> getInputs();

    public abstract String getXQuery();

    public boolean isRequired() {
        return required;
    }
}
