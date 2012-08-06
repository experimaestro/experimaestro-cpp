/*
 *
 *  This file is part of experimaestro.
 *  Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
 *
 *  experimaestro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  experimaestro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package sf.net.experimaestro.manager.js;

import com.jcraft.jsch.JSchException;
import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.connectors.ConnectorOptions;
import sun.org.mozilla.javascript.internal.ScriptableObject;

import java.net.URISyntaxException;

/**
 * Simple JavaScript interface to a connector object
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 12/6/12
 */
public class JSConnector extends ScriptableObject {
    private Connector connector;

    Connector getConnector() {
        return connector;
    }

    public void jsConstructor(String uri, ConnectorOptions options) throws JSchException, URISyntaxException {
        connector = Connector.create(uri, options);
    }

    @Override
    public String getClassName() {
        return "Connector";
    }

}
