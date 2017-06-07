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

import net.bpiwowar.xpm.manager.scripting.Expose;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.manager.scripting.Property;
import net.bpiwowar.xpm.scheduler.LauncherParameters;

import static java.lang.String.format;

/**
 * OAR specific parameters
 */
@Exposed
public class OARParameters extends LauncherParameters {
    static final OARParameters DEFAULT = new OARParameters();

    public int hosts = 1;

    public int cores = 1;

    public int nodes = 1;

    public int memory = 0;

    public long jobDuration = 30 * 24 * 60 * 60; // 24 days per default

    public OARParameters() {

    }

    public String oarSpecification() {
        long hours = this.jobDuration;

        long seconds = hours % 60;
        hours /= 60;

        long minutes = hours % 60;
        hours /= 60;

        return format("nodes=%d/host=%d/core=%d,walltime=%d:%02d:%02d", nodes, hosts, cores, hours, minutes, seconds);
    }

    @Expose
    public int getCores() {
        return cores;
    }

    @Expose
    public void setCores(int cores) {
        this.cores = cores;
    }

    @Expose
    public int getNodes() {
        return nodes;
    }

    @Expose
    public void setNodes(int nodes) {
        this.nodes = nodes;
    }

    @Expose
    public int getMemory() {
        return memory;
    }

    @Expose
    public void setMemory(int memory) {
        this.memory = memory;
    }

    @Expose
    public long getJobDuration() {
        return jobDuration;
    }

    @Expose
    public void setJobDuration(long jobDuration) {
        this.jobDuration = jobDuration;
    }

    @Expose
    public int getHosts() {
        return hosts;
    }

    @Expose
    public void setHosts(int hosts) {
        this.hosts = hosts;
    }
}
