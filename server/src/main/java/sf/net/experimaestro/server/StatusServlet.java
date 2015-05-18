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

import sf.net.experimaestro.exceptions.CloseException;
import sf.net.experimaestro.manager.experiments.Experiment;
import sf.net.experimaestro.manager.experiments.Experiment_;
import sf.net.experimaestro.scheduler.Resource;
import sf.net.experimaestro.scheduler.Resource.PrintConfig;
import sf.net.experimaestro.scheduler.ResourceState;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.scheduler.Transaction;
import sf.net.experimaestro.utils.CloseableIterator;
import sf.net.experimaestro.utils.arrays.ListAdaptator;
import sf.net.experimaestro.utils.log.Logger;

import javax.persistence.LockModeType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Gives the current task status
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class StatusServlet extends XPMServlet {
    public static final String RESOURCE_PATH = "/resource/";
    public static final String EXPERIMENTS_PATH= "/experiments";

    final static private Logger LOGGER = Logger.getLogger();

    private static final long serialVersionUID = 1L;

    private final Scheduler scheduler;

    public StatusServlet(ServerSettings serverSettings, Scheduler manager) {
        super(serverSettings);
        this.scheduler = manager;
    }

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {

        String localPath = request.getRequestURI().substring(
                request.getServletPath().length());

        if (localPath.equals("")) {
            final PrintWriter out = startHTMLResponse(response);
            header(out, "Resources");

            ArrayList<ResourceState> values = new ArrayList<>(
                    ListAdaptator.create(ResourceState.values()));
            out.println("<img id='connection' src='/images/disconnect.png' alt='[c]'>");

            out.println("<div id=\"delete-confirm\" title=\"Delete this job?\">\n" +
                    "<p><span class=\"ui-icon ui-icon-alert\" style=\"float:left; margin:0 7px 20px 0;\"></span>These items will be permanently deleted and cannot be recovered. Are you sure?</p>\n" +
                    "</div>\n");

            out.println("<div id='tab-main' class='tab'><ul><li><a href='#resources'>Resources</a></li><li><a href='#resource-detail'>Detail</a></li><li><a href='/status/experiments'>Experiments</a></li></ul>");
            out.println("<div id=\"resources\" class=\"tab\"><ul>");
            for (ResourceState state : values) {
                out.format("<li><a href=\"#state-%s\"><span>%s</span> (<span id=\"state-%s-count\">0</span>)</a></li>", state, state, state);
            }
            out.println("</ul>");

            for (ResourceState state : values) {

                out.format("<div id=\"state-%s\" class=\"xpm-resource-list\">", state);
                out.println("<ul>");
                Transaction.run(em -> {
                    try (final CloseableIterator<Resource> resources = scheduler.resources(em, EnumSet.of(state), LockModeType.NONE)) {
                        while (resources.hasNext()) {
                            Resource resource = resources.next();
                            if (resource.getState() == state) {
                                out.format("<li name=\"%s\" id=\"R%s\">", resource.getId(), resource.getId());
                                try {
                                    out.format("<img class='link' name='restart' alt='restart' src='/images/restart.png'/>");
                                    out.format("<img class='link' name='delete' alt='delete' src='/images/delete.png'/>");
                                    out.format("<a href=\"javascript:void(0)\">%s</a></li>", resource.getLocator());
                                } catch (Throwable t) {
                                    out.format("<b>Resource ID %s</b> without locator</li>", resource.getId());
                                }
                            }
                        }
                    } catch (CloseException e) {
                        LOGGER.warn("Error while closing the iterator");
                    }
                });
                out.println("</ul></div>");
            }
            out.println("</div>");

            out.println("<div id='resource-detail'><h2 id=\"resource-detail-title\"></h2>" +
                    "<div id=\"resource-detail-path\"></div>" +
                    "<div id=\"resource-detail-content\"></div></div>");
            out.println("</div>"); // end of tab

            out.println("</body></html>");
            return;
        }

        if (localPath.equals(EXPERIMENTS_PATH)) {
            final PrintWriter out = startHTMLResponse(response);
            Transaction.run(em -> {
                final CriteriaBuilder cb = em.getCriteriaBuilder();
                final CriteriaQuery<Experiment> cq = cb.createQuery(Experiment.class);
                final Root<Experiment> experiment = cq.from(Experiment.class);

                cq.orderBy(cb.desc(experiment.get(Experiment_.timestamp)));
                final List<Experiment> experiments = em.createQuery(cq).getResultList();

                for(Experiment o: experiments) {
                    out.format("<div>%s (%d)</div>", o.getName(), o.getTimestamp());
                }
            });
            out.print("<div>hello world</div>");
            return;
        }

        if (localPath.startsWith(RESOURCE_PATH)) {
            String resourceStringId = localPath.substring("/resource/".length());
            final long resourceId;
            try {
                resourceId = Long.parseLong(resourceStringId);
            } catch (NumberFormatException e) {
                show404(response, "Resource id [%s] is not a number", resourceStringId);
                return;
            }
            PrintWriter out = startHTMLResponse(response);

            Resource resource = Transaction.evaluate(em -> em.find(Resource.class, resourceId));
            header(out, String.format("Details of resource %s", resource.getLocator()));

            if (resource != null) {
                PrintConfig config = new PrintConfig();
                config.detailURL = request.getServletPath();
                out.format("<div class=\"resource\" name=\"%d\">%n", resourceId);
                resource.printXML(out, config);
                out.format("</div>");
            } else {
                out.format("Could not retrieve resource <b>%s</b>", resource.getLocator());
            }

            out.println("</body></html>");
            return;
        }

        // Not found
        error404(request, response);
    }
}