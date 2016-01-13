package net.bpiwowar.experimaestro.tasks;

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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;

/**
 * A task
 */
public abstract class AbstractTask {
    /** The working directory */
    protected File workingDirectory;

    /**
     * Execute the task
     * @param r The returned object pre-filled with values
     * @param progressListener Allows to report progress on a task
     * @return A json object corresponding to the task
     * @throws Throwable Any error that occurs should be reported through exceptions
     */
    public abstract JsonElement execute(JsonObject r, ProgressListener progressListener) throws Throwable;

    public void setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }
}
