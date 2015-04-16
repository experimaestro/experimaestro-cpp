package sf.net.experimaestro.manager.js;

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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;
import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.connectors.ConnectorOptions;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.utils.JSUtils;

import javax.tools.FileObject;
import java.net.URISyntaxException;
import java.nio.file.FileSystemException;
import java.nio.file.Path;

/**
 * Simple JavaScript interface to a connector object
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class JSConnector extends JSBaseObject implements JSConstructable {
    private Connector connector;

    public JSConnector() {
    }

    public JSConnector(Connector connector) {
        this.connector = connector;
    }

    @JSFunction(optional = 1)
    public JSConnector(String uriString, ConnectorOptions options) {
        try {
            this.connector = Connector.create(uriString, options);
        } catch (URISyntaxException e) {
            throw new XPMRuntimeException(e);
        }
    }

    Connector getConnector() {
        return connector;
    }

    @JSFunction
    public Path path(String filepath) throws FileSystemException {
        return getConnector().getMainConnector().resolveFile(filepath);
    }

    @Override
    public String getClassName() {
        return "Connector";
    }

}
