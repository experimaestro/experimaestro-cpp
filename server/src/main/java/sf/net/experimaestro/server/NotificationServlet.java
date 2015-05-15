package sf.net.experimaestro.server;

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

import sf.net.experimaestro.connectors.XPMProcess;
import sf.net.experimaestro.scheduler.*;
import sf.net.experimaestro.utils.Functional;

import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Handles notification
 */
public class NotificationServlet extends XPMServlet {
    private static final String END_OF_JOB = "eoj";
    private static final String PROGRESS = "progress";

    final Scheduler scheduler;

    public NotificationServlet(ServerSettings serverSettings, Scheduler scheduler) {
        super(serverSettings);
        this.scheduler = scheduler;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
        String[] parts = request.getPathInfo().split("/");
        if (parts.length < 3) {
            error404(request, resp);
            return;
        }

        final String jobIdString = parts[1];
        final String command = parts[2];
        long resourceId = Long.parseLong(jobIdString);

        switch (command) {
            case END_OF_JOB:
                resp.setContentType("application/json");
                resp.setStatus(HttpServletResponse.SC_ACCEPTED);

                Transaction.run(Functional.propagate(em -> {
                    final Job job = getJob(request, resp, em, resourceId);
                    if (job == null) return;
                    final XPMProcess process = job.getProcess();
                    if (process != null) {
                        try {
                            process.check();
                        } catch (Exception e) {
                            // FIXME: what to do here?
                        }
                    }

                }));

                return;
            case PROGRESS:
                resp.setContentType("application/json");
                resp.setStatus(HttpServletResponse.SC_ACCEPTED);

                double progress = Double.parseDouble(parts[3]);

                Transaction.run(Functional.propagate(em -> {
                    final Job job = getJob(request, resp, em, resourceId);
                    if (job.getState() == ResourceState.RUNNING) {
                        job.setProgress(progress);
                        Scheduler.get().notify(new SimpleMessage(Message.Type.PROGRESS, job));
                    }
                }));
        }

        return;
    }

    private Job getJob(HttpServletRequest request, HttpServletResponse resp, EntityManager em, long resourceId) throws IOException {
        final Job job = em.find(Job.class, resourceId);
        if (job == null) {
            error404(request, resp);
            return null;
        }
        return job;
    }

}
