/*
 *
 *  * This file is part of experimaestro.
 *  * Copyright (c) 2015 B. Piwowarski <benjamin@bpiwowar.net>
 *  *
 *  * experimaestro is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * experimaestro is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package net.bpiwowar.xpm.connectors;

import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.manager.scripting.Property;
import net.bpiwowar.xpm.scheduler.LauncherParameters;

import static java.lang.String.format;

/**
 * OAR specific parameters
 */
@Exposed
public class OARParameters extends LauncherParameters {
    static final OARParameters DEFAULT = new OARParameters(null);

    @Property
    public int cores = 1;

    @Property
    public int nodes = 1;

    @Property
    public int memory = 0;

    @Property
    public long jobDuration = 30 * 24 * 60 * 60; // 24 days per default

    public OARParameters(OARLauncher launcher) {
        super(launcher);
    }

    public String oarSpecification() {
        long hours = this.jobDuration;

        long seconds = hours % 60;
        hours /= 60;

        long minutes = hours % 60;
        hours /= 60;

        return format("nodes=%d/core=%d,walltime=%d:%02d:%02d", nodes, cores, hours, minutes, seconds);
    }
}
