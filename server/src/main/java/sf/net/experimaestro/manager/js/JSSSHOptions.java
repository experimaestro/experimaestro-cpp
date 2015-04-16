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

import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.annotations.JSFunction;
import sf.net.experimaestro.connectors.SSHOptions;

/**
 * JavaScript wrapper for SSH connection options
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 9/10/12
 */
public class JSSSHOptions extends JSConnectorOptions implements Wrapper {
    SSHOptions options = new SSHOptions();

    public SSHOptions getOptions() {
        return options;
    }

    @Override
    public String getClassName() {
        return "SSHOptions";
    }

    @JSFunction("set_stream_proxy")
    public void setStreamProxy(String uri, SSHOptions sshOptions) {
        options.setStreamProxy(uri, sshOptions);
    }

    @Override
    public Object unwrap() {
        return options;
    }
}
