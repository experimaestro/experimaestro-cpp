package sf.net.experimaestro.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import sf.net.experimaestro.rsrc.Task;
import sf.net.experimaestro.rsrc.TaskManager;

/**
 * Browse the list of tasks
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class TasksServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final TaskManager manager;

	public TasksServlet(TaskManager manager) {
		this.manager = manager;
	}

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException,
			IOException {
		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);
		final PrintWriter out = response.getWriter();

		out.println("<html><head><title>Experimaestro - Task browser</title></head><body>");
		
		out.println("<h1>Available tasks</h1>");
		out.println("<ul>");
		for (Task task : manager.tasks()) {
			out.format("<li>%s</li>", task);
		}
		out.println("</ul>");

		out.println("</body></html>");
	}
}