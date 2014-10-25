package sf.net.experimaestro.manager;

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

import org.w3c.dom.Node;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.utils.log.Logger;

import javax.xml.namespace.NamespaceContext;
import java.util.Iterator;

public class NSContext implements NamespaceContext {
    final static private Logger LOGGER = Logger.getLogger();
    private Node node;

    public NSContext(Node node) {
        this.node = node;
    }

    @Override
    public String getNamespaceURI(String prefix) {
        String uri = node.lookupNamespaceURI(prefix);
        if (uri == null)
            uri = Manager.PREDEFINED_PREFIXES.get(prefix);
        if (uri == null)
            throw new XPMRuntimeException("Prefix %s not bound", prefix);
        LOGGER.debug("Prefix %s maps to %s", prefix, uri);
        return uri;
    }

    @Override
    public String getPrefix(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<?> getPrefixes(String arg0) {
        throw new UnsupportedOperationException();
    }
}