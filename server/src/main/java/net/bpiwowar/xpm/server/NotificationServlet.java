package net.bpiwowar.xpm.server;

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

import net.bpiwowar.xpm.connectors.XPMProcess;
import net.bpiwowar.xpm.scheduler.Job;
import net.bpiwowar.xpm.scheduler.Resource;
import net.bpiwowar.xpm.scheduler.ResourceState;
import net.bpiwowar.xpm.scheduler.Scheduler;
import net.bpiwowar.xpm.scheduler.ResourceMessage;
import net.bpiwowar.xpm.tasks.ServerCommand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Handles notification
 */
public class NotificationServlet extends XPMServlet {
    static final private Logger LOGGER = LogManager.getFormatterLogger();

    private static final String END_OF_JOB = "eoj";

    private static final String PROGRESS = "progress";

    /** On EOJ notification, by default we retry 10 times */
    final int retries = 10;

    final Scheduler scheduler;

    public NotificationServlet(ServerCommand.ServerSettings serverSettings, Scheduler scheduler) {
        super(serverSettings);
        this.scheduler = scheduler;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String[] parts = request.getPathInfo().split("/+");
            if (parts.length < 3) {
                error404(request, resp);
                return;
            }

            final String jobIdString = parts[1];
            final String command = parts[2];
            long resourceId = Long.parseLong(jobIdString);

            final Job job;
            try {
                job = getJob(request, resp, resourceId);
            } catch (SQLException e) {
                error404(request, resp);
                return;
            }
            if (job == null) {
                error404(request, resp);
                return;
            }

            switch (command) {
                case END_OF_JOB: {
                    resp.setContentType("application/json");
                    resp.setStatus(HttpServletResponse.SC_ACCEPTED);

                    final XPMProcess process = job.getProcess();
                    if (process != null) {
                        try {
                            int count = 0;
                            // Loop to allow some latency
                            while (!process.check(true, 0) && count++ < retries) {
                                synchronized (this) {
                                    // Sleep for half a second before retrying
                                    Thread.sleep(500);
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.error(() -> String.format("Error while processing the end of job notification (job %d)", resourceId), e);
                            // FIXME: what to do here?
                        }
                    }
                }

                return;
                case PROGRESS: {
                    resp.setContentType("application/json");
                    resp.setStatus(HttpServletResponse.SC_ACCEPTED);

                    double progress = Double.parseDouble(parts[3]);

                    if (job.getState() == ResourceState.RUNNING) {
                        job.setProgress(progress);
                        Scheduler.get().notify(ResourceMessage.progress(job));
                    }
                }
            }

        } finally {
            Scheduler.closeConnection();
        }

    }

    private Job getJob(HttpServletRequest request, HttpServletResponse resp, long resourceId) throws IOException, SQLException {
        final Job job = (Job) Resource.getById(resourceId);
        if (job == null) {
            error404(request, resp);
            return null;
        }
        return job;
    }

}
