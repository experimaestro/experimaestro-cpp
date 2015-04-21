package sf.net.experimaestro.connectors;

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

import java.io.IOException;
import java.nio.file.Path;

/**
 * A default launcher uses the SingleHostConnector process builders
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class DefaultLauncher implements Launcher {
    @Override
    public AbstractProcessBuilder processBuilder(SingleHostConnector connector) {
        return connector.processBuilder();
    }

    @Override
    public XPMScriptProcessBuilder scriptProcessBuilder(SingleHostConnector connector, Path scriptFile) throws IOException {
        return connector.scriptProcessBuilder(scriptFile);
    }
}
