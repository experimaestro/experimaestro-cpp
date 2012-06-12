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

package sf.net.experimaestro.scheduler;

import bpiwowar.argparser.utils.Formatter;
import bpiwowar.argparser.utils.Output;
import com.sleepycat.persist.model.Persistent;
import sf.net.experimaestro.utils.arrays.ListAdaptator;

/**
 * A command line launcher with OAR
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Persistent
public class OARLauncher extends Launcher {

    private Object oarCommand = "oarsub";


    @Override
    public String getCommand(String identifier) {
        final String id = CommandLineTask.protect(identifier, "\"");
        return String.format("%s --stdout=\"%s.out\" --stderr=\"%2$s.err\" \"%2s.run\" ",
                oarCommand, id);
    }
}
