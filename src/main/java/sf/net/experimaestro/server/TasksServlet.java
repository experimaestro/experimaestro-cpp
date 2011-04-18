package sf.net.experimaestro.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import sf.net.experimaestro.manager.DotName;
import sf.net.experimaestro.manager.NamedParameter;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.manager.TaskRepository;
import sf.net.experimaestro.scheduler.TaskManager;
import sf.net.experimaestro.utils.log.Logger;

/**
 * Servlet to browse the list of tasks
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class TasksServlet extends XPMServlet {
	final static private Logger LOGGER = Logger.getLogger();

	private static final long serialVersionUID = 1L;
	
	private final TaskManager manager;
	private final TaskRepository repository;

	public TasksServlet(TaskRepository repository, TaskManager manager) {
		this.repository = repository;
		this.manager = manager;
	}

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		String localPath = request.getRequestURI().substring(
				request.getServletPath().length());

		if (localPath.equals("")) {
			final PrintWriter out = response.getWriter();

			response.setContentType("text/html");
			response.setStatus(HttpServletResponse.SC_OK);
			response.setCharacterEncoding(ENCODING);
			out.println("<html><head><title>Experimaestro - Task browser</title></head><body>");

			out.println("<h1>Available tasks</h1>");
			out.println("<ul>");

			for (TaskFactory task : repository.tasks()) {
				QName id = task.getId();
				out.format(
						"<li><a href=\"%s/show?ns=%s&amp;name=%s\">%s</a></li>",
						request.getServletPath(),
						urlEncode(id.toString()),
						id.getLocalPart(), id);
			}
			out.println("</ul>");

			out.println("</body></html>");
			return;
		}

		if (localPath.equals("/show")) {
			response.setContentType("text/html");
			response.setStatus(HttpServletResponse.SC_OK);
			response.setCharacterEncoding(ENCODING);
			final PrintWriter out = response.getWriter();

			String name = request.getParameter("name");
			String ns = request.getParameter("ns");
			TaskFactory factory = repository.get(new QName(ns, name));
			out.format(
					"<html><head><title>Experimaestro - Task browser (task {%s}%s)</title></head><body>",
					ns, name);
			out.format("<h1> Task %s (%s)</h1>", name, ns);
			if (factory == null) {
				out.println("<div style='text-color: red'>Unknown task</div>");
			} else {
				out.println("<h2>Informations</h2>");
				out.format("<div><b>Version:</b> %s</div>%n",
						factory.getVersion());
				out.println("<h2>Input</h2>");
				printParameters(out, factory);

				out.println("<h2>Output</h2>");
				out.println("<h2>Documentation</h2>");
				out.println(factory.getDocumentation());
			}
			out.println("</body></html>");

			return;
		}

		ContentServlet.error404(request, response);
	}

	private void printParameters(final PrintWriter out, TaskFactory factory) {
		out.println("<dl>");

		for (Entry<DotName, NamedParameter> input : factory.getInputs()
				.entrySet()) {
			out.format("<dt>%s (%s)%s</dt><dd>%s</dd>", input.getKey(), input
					.getValue().getType(),
					input.getValue().isOptional() ? " - optional" : "", input
							.getValue().getDocumentation());
		}
		out.println("</dl>");

		Map<String, TaskFactory> subtasks = factory.getSubtasks();
		if (subtasks != null)
			for (Entry<String, TaskFactory> entry : subtasks.entrySet()) {
				out.format("<h3>%s</h3><div style='left-margin: 1em'>", entry.getKey());
				printParameters(out, entry.getValue());
				out.println("</div>");
			}
	}
}