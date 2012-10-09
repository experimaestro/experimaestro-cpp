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

package sf.net.experimaestro.manager.js;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;
import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.connectors.ConnectorOptions;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.utils.JSUtils;

import java.net.URISyntaxException;

/**
 * Simple JavaScript interface to a connector object
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 12/6/12
 */
public class JSConnector extends ScriptableObject {
    private Connector connector;


    public JSConnector() {}

    public JSConnector(Connector connector) {
        this.connector = connector;
    }


    Connector getConnector() {
        return connector;
    }


    public static JSConnector jsConstructor(Context cx, Object[] args,
                                            Function ctorObj,
                                            boolean inNewExpr) {
        final int nbArgs = args.length;
        if (nbArgs < 1 || nbArgs > 2)
            throw new IllegalArgumentException("Connector constructor takes one or two arguments");

        final String uriString = Context.toString(args[0]);

        ConnectorOptions options = null;
        if (nbArgs == 2) {
            options = ((JSConnectorOptions) JSUtils.unwrap(args[1])).getOptions();
        }

        try {
            Connector connector = Connector.create(uriString, options);
            return new JSConnector(connector);
        } catch (URISyntaxException e) {
            throw new ExperimaestroRuntimeException(e);
        }
    }


    @Override
    public String getClassName() {
        return "Connector";
    }

}
