/*
 *
 *  This file is part of experimaestro.
 *  Copyright (c) 2011 B. Piwowarski <benjamin@bpiwowar.net>
 *
 *  experimaestro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  experimaestro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package sf.net.experimaestro.server;

import com.sun.org.apache.xerces.internal.impl.xs.XSElementDecl;
import com.sun.org.apache.xerces.internal.xs.XSComplexTypeDefinition;
import sf.net.experimaestro.manager.*;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.log.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map.Entry;

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
					.getDefaultModule() : repository
					.getModule(new QName(ns, name));

			out.println("<html><head><title>Experimaestro - Task browser</title></head><body>");

			printModules(request, out, module.getSubmodules());
			printTasks(request, out, module.getFactories());

			out.println("</body></html>");
			return;
		}

		if (localPath.equals("/type")) {
			final PrintWriter out = startHTMLResponse(response);

			String name = request.getParameter("name");
			String ns = request.getParameter("ns");
			out.format(
					"<html><head><title>Experimaestro - Type browser (type {%s}%s)</title></head><body>",
					ns, name);
			out.format("<h1> Type %s (%s)</h1>", name, ns);

			XSElementDecl xmlElement = repository.getXMLElement(new QName(ns, name));
			if (xmlElement != null) {
				out.println(xmlElement.getTypeDefinition());
				XSComplexTypeDefinition definition = xmlElement.getEnclosingCTDefinition();
				if (definition != null) {
					out.println(definition.toString());
				}
			} else {
				out.println("<div style='text-color: red'>Unknown type</div>");
			}
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
				printParameters(request, out, factory);

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
				out.format("<li><a href=\"%s?ns=%s&amp;name=%s\">%s</a></li>",
						request.getServletPath(),
						urlEncode(id.getNamespaceURI()), id.getLocalPart(), id);
			}
		}
		out.println("</ul>");
	}

	/**
	 * Print the input of a TaskFactory
	 * 
	 * @param out
	 * @param factory
	 */
	private void printParameters(HttpServletRequest request,
			final PrintWriter out, TaskFactory factory) {
		out.println("<dl>");

		for (Entry<String, Input> entry : factory.getInputs().entrySet()) {
			Input input = entry.getValue();
			final QName type = input.getType();
			XSElementDecl declaration = repository.getXMLElement(type);
			if (declaration == null)
				out.format("<dt class='%s'>%s (%s)</dt><dd>",
						input.isOptional() ? "optional" : "required",
						type.getLocalPart(), type.getNamespaceURI());
			else
				out.format(
						"<dt class='%s'><a href=\"%s/type?ns=%s&amp;name=%s\">%s (%s)</a></dt><dd>",
						input.isOptional() ? "optional" : "required",
						request.getServletPath(),
						urlEncode(type.getNamespaceURI()), type.getLocalPart(),
						type.getLocalPart(), type.getNamespaceURI());

			input.printHTML(out);
			out.println("</dd>");
		}
		out.println("</dl>");

	}
}