package sf.net.experimaestro.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import sf.net.experimaestro.scheduler.Resource;
import sf.net.experimaestro.scheduler.Task;
import sf.net.experimaestro.scheduler.TaskManager;

/**
 * Gives the current task status
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class StatusServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final TaskManager manager;

	public StatusServlet(TaskManager manager) {
		this.manager = manager;
	}

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException,
			IOException {
		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);
		final PrintWriter out = response.getWriter();

		out.println("<h1>Waiting tasks</h1>");
		out.println("<ul>");
		for (Task task : manager.tasks()) {
			out.format("<li>%s</li>", task);
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
	}
}