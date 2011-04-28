package sf.net.experimaestro.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import sf.net.experimaestro.manager.Input;
import sf.net.experimaestro.manager.Module;
import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.log.Logger;

/**
 * Servlet to browse the list of tasks
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class TasksServlet extends XPMServlet {
	@SuppressWarnings("unused")
	final static private Logger LOGGER = Logger.getLogger();

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private final Scheduler manager;
	private final Repository repository;

	public TasksServlet(Repository repository, Scheduler manager) {
		this.repository = repository;
		this.manager = manager;
	}

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		String localPath = request.getRequestURI().substring(
				request.getServletPath().length());

		if (localPath.equals("")) {
			final PrintWriter out = startHTMLResponse(response);
			String name = request.getParameter("name");
			String ns = request.getParameter("ns");
			Module module = name == null || ns == null ? repository
					.getMainModule() : repository
					.getModule(new QName(ns, name));

			out.println("<html><head><title>Experimaestro - Task browser</title></head><body>");

			printModules(request, out, module.getSubmodules());
			printTasks(request, out, module.getFactories());

			out.println("</body></html>");
			return;
		}

		if (localPath.equals("/show")) {
			final PrintWriter out = startHTMLResponse(response);

			String name = request.getParameter("name");
			String ns = request.getParameter("ns");
			TaskFactory factory = repository.getFactory(new QName(ns, name));
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

	private void printTasks(HttpServletRequest request, final PrintWriter out,
			Collection<TaskFactory> tasks) {
		out.println("<h1>Tasks without module</h1>");
		out.println("<ul>");

		for (TaskFactory task : tasks) {
			QName id = task.getId();
			out.format("<li><a href=\"%s/show?ns=%s&amp;name=%s\">%s</a></li>",
					request.getServletPath(), urlEncode(id.getNamespaceURI()),
					id.getLocalPart(), id);
		}
		out.println("</ul>");
	}

	private void printModules(HttpServletRequest request,
			final PrintWriter out, final Collection<Module> modules) {
		out.println("<h1>Modules</h1><ul>");
		for (Module module : modules) {
			if (module.getParent() != null) {
				QName id = module.getId();
				out.format(
						"<li><a href=\"%s?ns=%s&amp;name=%s\">%s</a></li>",
						request.getServletPath(),
						urlEncode(id.getNamespaceURI()), id.getLocalPart(), id);
			}
		}
		out.println("</ul>");
	}

	private void printParameters(final PrintWriter out, TaskFactory factory) {
		out.println("<dl>");

		for (Entry<String, Input> entry : factory.getInputs().entrySet()) {
			Input input = entry.getValue();
			out.format("<dt class='%s'>%s (%s)</dt><dd>",
					input.isOptional() ? "optional" : "required", input
							.getType().getLocalPart(), input.getType()
							.getNamespaceURI());
			input.printHTML(out);
			out.println("</dd>");
		}
		out.println("</dl>");

	}
}