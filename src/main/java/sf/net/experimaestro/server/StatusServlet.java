package sf.net.experimaestro.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import sf.net.experimaestro.scheduler.Resource;
import sf.net.experimaestro.scheduler.Job;
import sf.net.experimaestro.scheduler.TaskManager;

/**
 * Gives the current task status
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class StatusServlet extends XPMServlet {
	private static final long serialVersionUID = 1L;
	private final TaskManager manager;

	public StatusServlet(TaskManager manager) {
		this.manager = manager;
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
			for (Job task : manager.tasks()) {
				out.format("<li><a href=\"%s/job?id=%s\">%s</a></li>",
						request.getServletPath(), urlEncode(task.getIdentifier()), task);
			}
			out.println("</ul>");

			out.println("<h1>List of resources (generated)</h1>");
			out.println("<ul>");
			for (Resource resource : manager.resources()) {
				if (resource.isGenerated())
					out.format("<li>[%s] %s</li>", resource.getClass(),
							resource);
			}
			out.println("</ul>");

			out.println("<h1>List of resources (not generated)</h1>");
			out.println("<ul>");
			for (Resource resource : manager.resources()) {
				if (!resource.isGenerated())
					out.format("<li>[%s] %s</li>", resource.getClass(),
							resource);
			}
			out.println("</ul>");

			out.println("</body></html>");
			return;
		}

		if (localPath.equals("/job")) {
			PrintWriter out = startHTMLResponse(response);
			String jobId = request.getParameter("id");

			out.format("<html><head><title>Experimaestro - Details of job %s</title></head><body>", jobId);
			out.format("<h1>Details of Job %s</h1>", jobId);
			
			
			out.println("</body></html>");
			return;
		}

		// Not found
		ContentServlet.error404(request, response);
	}
}