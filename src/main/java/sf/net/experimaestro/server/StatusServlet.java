package sf.net.experimaestro.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import sf.net.experimaestro.scheduler.Dependency;
import sf.net.experimaestro.scheduler.Job;
import sf.net.experimaestro.scheduler.Resource;
import sf.net.experimaestro.scheduler.ResourceState;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.Time;
import sf.net.experimaestro.utils.arrays.ListAdaptator;

import com.sleepycat.je.DatabaseException;

/**
 * Gives the current task status
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class StatusServlet extends XPMServlet {
	private static final long serialVersionUID = 1L;
	private final Scheduler scheduler;
	static DateFormat longDateFormat = DateFormat.getDateTimeInstance(
			DateFormat.FULL, DateFormat.FULL);

	public StatusServlet(Scheduler manager) {
		this.scheduler = manager;
	}

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		String localPath = request.getRequestURI().substring(
				request.getServletPath().length());

		if (localPath.equals("")) {
			final PrintWriter out = startHTMLResponse(response);
			out.println("<html><head><title>Experimaestro - Resources</title></head><body>");

			ArrayList<ResourceState> values = new ArrayList<ResourceState>(
					ListAdaptator.create(ResourceState.values()));
			values.add(null);
			for (ResourceState state : values) {
				out.format("<h1>Resources in state %s</h1>", state);
				out.println("<ul>");
				for (Resource resource : scheduler.resources()) {
					if (resource.getState() == state)
						out.format(
								"<li><a href=\"%s/resource?id=%s\">%s</a></li>",
								request.getServletPath(),
								urlEncode(resource.getIdentifier()),
								resource.getIdentifier());
				}
				out.println("</ul>");
			}

			out.println("</body></html>");
			return;
		}

		if (localPath.equals("/resource")) {
			PrintWriter out = startHTMLResponse(response);
			String jobId = request.getParameter("id");

			out.format(
					"<html><head><title>Experimaestro - Details of resource %s</title></head><body>",
					jobId);

			Resource resource;
			try {
				resource = scheduler.getResource(jobId);
			} catch (DatabaseException e) {
				throw new IOException(e);
			}

			if (resource instanceof Job) {
				Job job = (Job) resource;
				out.format("<h1>Details of job <code>%s</code></h1>", jobId);
				out.format("<div><b>Status</b>: %s</div>", job.getState());
				out.format("<div><b>Lock</b>: %s</div>",
						job.isLocked() ? "Locked" : "Not locked");
				out.format("<div>%d writer(s) and %d reader(s)</div>",
						job.getReaders(), job.getWriters());

				if (job.getState() == ResourceState.DONE
						|| job.getState() == ResourceState.ERROR
						|| job.getState() == ResourceState.RUNNING) {
					long start = job.getStartTimestamp();
					long end = job.getState() == ResourceState.RUNNING ? System
							.currentTimeMillis() : job.getEndTimestamp();

					out.format("<div>Started: %s</div>",
							longDateFormat.format(new Date(start)));
					if (job.getState() != ResourceState.RUNNING)
						out.format("<div>Ended: %s</div>",
								longDateFormat.format(new Date(end)));
					out.format("<div>Duration: %s</div>",
							Time.formatTimeInMilliseconds(end - start));
				}

				TreeMap<String, Dependency> dependencies = job
						.getDependencies();
				if (!dependencies.isEmpty()) {
					out.format("<h2>Dependencies</h2><ul>");
					for (Entry<String, Dependency> entry : dependencies
							.entrySet()) {
						String dependency = entry.getKey();
						Dependency status = entry.getValue();
						out.format(
								"<li><a href=\"%s/resource?id=%s\">%s</a>: %s</li>",
								request.getServletPath(),
								urlEncode(dependency), dependency,
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