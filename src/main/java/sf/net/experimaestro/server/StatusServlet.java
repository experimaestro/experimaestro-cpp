package sf.net.experimaestro.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map.Entry;
import java.util.SortedMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import sf.net.experimaestro.scheduler.Job;
import sf.net.experimaestro.scheduler.Job.DependencyStatusCache;
import sf.net.experimaestro.scheduler.Resource;
import sf.net.experimaestro.scheduler.Scheduler;

/**
 * Gives the current task status
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class StatusServlet extends XPMServlet {
	private static final long serialVersionUID = 1L;
	private final Scheduler scheduler;

	public StatusServlet(Scheduler manager) {
		this.scheduler = manager;
	}

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		String localPath = request.getRequestURI().substring(
				request.getServletPath().length());

		if (localPath.equals("")) {
			final PrintWriter out = startHTMLResponse(response);
			out.println("<html><head><title>Experimaestro - Jobs</title></head><body>");

			out.println("<h1>Waiting jobs</h1>");
			out.println("<ul>");
			for (Job task : scheduler.tasks()) {
				out.format("<li><a href=\"%s/resource?id=%s\">%s</a></li>",
						request.getServletPath(),
						urlEncode(task.getIdentifier()), task.getIdentifier());
			}
			out.println("</ul>");

			out.println("<h1>List of resources (generated)</h1>");
			out.println("<ul>");
			for (Resource resource : scheduler.resources()) {
				if (resource.isGenerated())
					out.format("<li>[%s] %s</li>", resource.getClass(),
							resource);
			}
			out.println("</ul>");

			out.println("<h1>List of resources (not generated)</h1>");
			out.println("<ul>");
			for (Resource resource : scheduler.resources()) {
				if (!resource.isGenerated())
					out.format("<li>[%s] %s</li>", resource.getClass(),
							resource);
			}
			out.println("</ul>");

			out.println("</body></html>");
			return;
		}

		if (localPath.equals("/resource")) {
			PrintWriter out = startHTMLResponse(response);
			String jobId = request.getParameter("id");

			out.format(
					"<html><head><title>Experimaestro - Details of resource %s</title></head><body>",
					jobId);

			Resource resource = scheduler.getResource(jobId);

			if (resource instanceof Job) {
				Job job = (Job) resource;
				out.format("<h1>Details of job <code>%s</code></h1>", jobId);
				out.format("<div><b>Status</b>: %s</div>",
						job.isGenerated() ? "Generated" : "Not generated");
				out.format("<div><b>Lock</b>: %s</div>",
						job.isLocked() ? "Locked" : "Not locked");

				SortedMap<Resource, DependencyStatusCache> dependencies = job
						.getDependencies();
				if (!dependencies.isEmpty()) {
					out.format("<h2>Dependencies</h2><ul>");
					for (Entry<Resource, DependencyStatusCache> entry : dependencies
							.entrySet()) {
						Resource dependency = entry.getKey();
						DependencyStatusCache status = entry.getValue();
						out.format("<li><a href=\"%s/resource?id=%s\">%s</a>: %s</li>",
								request.getServletPath(),
								urlEncode(dependency.getIdentifier()),
								dependency.getIdentifier(),
								status.getType());
					}
					out.println("</ul>");
				}
			}

			out.println("</body></html>");
			return;
		}

		// Not found
		ContentServlet.error404(request, response);
	}
}