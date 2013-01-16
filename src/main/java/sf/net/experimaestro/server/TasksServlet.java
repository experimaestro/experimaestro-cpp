/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.server;

import com.sun.org.apache.xerces.internal.impl.xs.XSElementDecl;
import com.sun.org.apache.xerces.internal.xs.XSComplexTypeDefinition;
import com.sun.org.apache.xerces.internal.xs.XSElementDeclaration;
import sf.net.experimaestro.manager.*;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.log.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

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

    private final Repository defaultRepository;

    private static Map<String, Repository> repositories = new TreeMap<>();


    static public void updateRepository(String id, Repository repository) {
        repositories.put(id, repository);
    }

    public TasksServlet(Repository repository, Scheduler manager) {
        this.defaultRepository = repository;
        this.manager = manager;
    }

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {

        Map<String, String> query = new TreeMap<>();
        String localPath = request.getRequestURI().substring(
                request.getServletPath().length());

        String repositoryId = request.getParameter("repository");
        Repository repository = repositoryId != null ?
                repositories.get(repositoryId) : defaultRepository;
        if (repositoryId != null) {
            query.put("repository", repositoryId);
        }


        if (localPath.equals("")) {
            final PrintWriter out = startHTMLResponse(response);
            String name = request.getParameter("name");
            String ns = request.getParameter("ns");

            Module module = name == null || ns == null ? repository
                    .getDefaultModule() : repository
                    .getModule(new QName(ns, name));

            out.println("<html><head><title>Experimaestro - Task browser</title></head><body>");

            printRepositories(request, out);

            printModules(request, out, query, module.getSubmodules());
            printTasks(request, out, query, module.getFactories());

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

            XSElementDeclaration xmlElement = repository.getXMLElement(new QName(ns, name));
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
                printParameters(request, out, factory, repository);

                out.println("<h2>Output</h2>");
                out.println("<h2>Documentation</h2>");
                out.println(factory.getDocumentation());
            }
            out.println("</body></html>");

            return;
        }

        ContentServlet.error404(request, response);
    }

    private void printRepositories(HttpServletRequest request, PrintWriter out) {
        out.println("<h1>Repositories</h1>");
        out.println("<ul>");
        for (Map.Entry<String, Repository> repository : repositories.entrySet()) {
            out.format("<li><a href=\"%s?repository=%s\">%s</a> [clear]</li>%n",
                    request.getServletPath(), urlEncode(repository.getKey()),
                    escapeHtml(repository.getKey()));
        }
        out.println("</ul>");
    }

    private void printTasks(HttpServletRequest request, final PrintWriter out, Map<String, String> query,
                            Collection<TaskFactory> tasks) {
        out.println("<h1>Tasks without module</h1>");
        out.println("<ul>");

        for (TaskFactory task : tasks) {
            QName id = task.getId();
            out.format("<li><a href=\"%s\">%s</a></li>",
                    makeURI(request.getServletPath() + "/show", query, "ns", id.getNamespaceURI(), "name", id.getLocalPart()),
                    id);
        }
        out.println("</ul>");
    }

    private void printModules(HttpServletRequest request,
                              final PrintWriter out, Map<String, String> query, final Collection<Module> modules) {
        out.println("<h1>Modules</h1><ul>");
        for (Module module : modules) {
            if (module.getParent() != null) {
                QName id = module.getId();
                out.format("<li><a href=\"%s\">%s</a></li>",
                        makeURI(request.getServletPath(), query, "ns", id.getNamespaceURI(), "name", id.getLocalPart()),
                        id);
            }
        }
        out.println("</ul>");
    }

    private String makeURI(String path, Object... objects) {
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        sb.append(path);
        for (int i = 0; i < objects.length; i++) {
            if (first)
                sb.append('?');
            else
                sb.append("&amp;");

            first = false;

            if (objects[i] instanceof Map) {
                boolean _first = true;
                for (Map.Entry<String, String> entry : ((Map<String, String>) objects[i]).entrySet()) {
                    if (!_first) sb.append("&amp;");
                    else _first = false;
                    sb.append(entry.getKey());
                    sb.append('=');
                    sb.append(entry.getValue());
                }
            } else {
                sb.append(objects[i]);
                sb.append('=');
                i++;
                sb.append(urlEncode(objects[i].toString()));
            }

        }
        return sb.toString();
    }

    /**
     * Print the input of a TaskFactory
     *
     * @param out
     * @param factory
     * @param repository
     */
    private void printParameters(HttpServletRequest request,
                                 final PrintWriter out, TaskFactory factory, Repository repository) {
        out.println("<dl>");

        for (Entry<String, Input> entry : factory.getInputs().entrySet()) {
            Input input = entry.getValue();
            final Type type = input.getType();
            final String optString = input.isOptional() ? "optional" : "required";
            if (type == null) {
                out.format("<dt class='%s'><u>%s</u> [<b>no type</b>]</dt><dd>",
                        optString, entry.getKey());

            } else {

                if (type instanceof ValueType) {
                    final QName valueType = ((ValueType) type).getValueType();
                    out.format("<dt class='%s'><u>%s</u> <b>value</b> (%s)</dt><dd>",
                            optString, entry.getKey(), valueType);
                } else {
                    XSElementDecl declaration = repository.getXMLElement(type.qname());
                    if (declaration == null)
                        out.format("<dt class='%s'><u>%s</u> %s</dt><dd>",
                                optString, entry.getKey(), type);
                    else
                        out.format(
                                "<dt class='%s'><a href=\"%s/type?ns=%s&amp;name=%s\"><u>%s</u> %s</a></dt><dd>",
                                optString,
                                request.getServletPath(),
                                urlEncode(type.getNamespaceURI()),
                                entry.getKey(),
                                type);
                }
            }

            input.printHTML(out);
            out.println("</dd>");
        }
        out.println("</dl>");

    }
}