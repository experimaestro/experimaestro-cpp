/*
 *
 *  * This file is part of experimaestro.
 *  * Copyright (c) 2016 B. Piwowarski <benjamin@bpiwowar.net>
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

package net.bpiwowar.xpm.manager.tasks;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.String.format;

/**
 * Listens to progress on a given task
 */
public class ProgressListener {
    private final ExecutorService pool;
    String baseURL;

    public ProgressListener(String baseURL) {
        this.baseURL = baseURL;
        this.pool = Executors.newSingleThreadScheduledExecutor();
    }

    public ProgressListener() {
        this(null);
    }

    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    /**
     * Update the progress of the task
     *
     * @param progress A progress value between 0 and 1
     */
    public void progress(float progress) {
        if (baseURL != null) {
            // Asynchronous call
            pool.submit(() -> {
                try {
                    URL progressURL = new URL(format("%s/progress/%.f", baseURL, progress));
                    final URLConnection connection = progressURL.openConnection();
                    connection.connect();
                } catch (IOException e) {
                }
            });
        }
    }


}
