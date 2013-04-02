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

import org.apache.xerces.impl.xs.XSElementDecl;
import org.apache.xerces.xs.*;
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

    public TasksServlet(ServerSettings serverSettings, Repository repository, Scheduler manager) {
        super(serverSettings);
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
        Status status = new Status(request, query);

        String moduleName = request.getParameter("module");
        Module module = moduleName == null ? repository
                .getDefaultModule() : repository
                .getModule(QName.parse(moduleName));
        if (moduleName != null) {
            query.put("module", moduleName);
        }

        if (localPath.equals("")) {
            final PrintWriter out = startHTMLResponse(response);

            header(out, "Task browser");

            printRepositories(request, out);

            printModules(status, out, module.getSubmodules());
            printTasks(status, out, module.getFactories());

            out.println("</body></html>");
            return;
        }

        if (localPath.equals("/type")) {
            final PrintWriter out = startHTMLResponse(response);
            String type = request.getParameter("type");

            header(out, String.format("browser (type 7%s)", moduleName));
            out.format("<h1>Type %s</h1>", type);

            XSElementDeclaration xmlElement = repository.getXMLElement(QName.parse(type));
            if (xmlElement != null) {
                XSTypeDefinition definition = xmlElement.getTypeDefinition();
                if (definition != null) {
                    print(out, definition);
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
            header(out, String.format("Task browser (task {%s}%s)", ns, name));
            out.format("<h1> Task %s (%s)</h1>", name, ns);
            if (factory == null) {
                out.println("<div style='text-color: red'>Unknown task</div>");
            } else {
                out.println("<h2>Informations</h2>");
                out.format("<div><b>Version:</b> %s</div>%n",
                        factory.getVersion());
                out.println("<h2>Input</h2>");
                printParameters(status, out, factory, repository);

                out.println("<h2>Output</h2>");
                out.println("<h2>Documentation</h2>");
                out.println(factory.getDocumentation());
            }
            out.println("</body></html>");

            return;
        }


        error404(request, response);
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

    private void printTasks(Status status, final PrintWriter out,
                            Collection<TaskFactory> tasks) {
        out.println("<h1>Tasks without module</h1>");
        out.println("<ul>");

        for (TaskFactory task : tasks) {
            QName id = task.getId();
            out.format("<li><a href=\"%s\">%s</a></li>",
                    status.makeURI("/show", "ns", id.getNamespaceURI(), "name", id.getLocalPart()),
                    id);
        }
        out.println("</ul>");
    }

    private void printModules(Status status, final PrintWriter out, final Collection<Module> modules) {
        out.println("<h1>Modules</h1><ul>");
        for (Module module : modules) {
            if (module.getParent() != null) {
                QName id = module.getId();
                out.format("<li><a href=\"%s\">%s</a></li>",
                        status.makeURI("", "module", id.toString()),
                        id
                );
            }
        }
        out.println("</ul>");
    }


    /**
     * Print the input of a TaskFactory
     *
     * @param status
     * @param out
     * @param factory
     * @param repository
     */
    private void printParameters(Status status,
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
                    final QName valueType = type.qname();
                    out.format("<dt class='%s'><u>%s</u> <b>value</b> (%s)</dt><dd>",
                            optString, entry.getKey(), valueType);
                } else {
                    XSElementDecl declaration = repository.getXMLElement(type.qname());
                    if (declaration == null)
                        out.format("<dt class='%s'><u>%s</u> %s</dt><dd>",
                                optString, entry.getKey(), type);
                    else
                        out.format(
                                "<dt class='%s'><a href=\"%s\"><u>%s</u> %s</a></dt><dd>",
                                optString,
                                status.makeURI("/type", "type", type.toString()),
                                entry.getKey(), type);
                }
            }

            input.printHTML(out);
            out.println("</dd>");
        }
        out.println("</dl>");

    }

    static public class Status {
        HttpServletRequest request;
        Map<String, String> query;

        public Status(HttpServletRequest request, Map<String, String> query) {
            this.request = request;
            this.query = query;
        }


        private String makeURI(String path, String... objects) {
            path = request.getServletPath() + path;
            StringBuilder sb = new StringBuilder();
            sb.append(path);

            final TreeMap<String, String> map = new TreeMap<>(query);
            for (int i = 0; i < objects.length; i += 2) {
                map.put(objects[i], objects[i + 1]);
            }

            boolean first = true;
            for (Entry<String, String> value : map.entrySet()) {
                if (first) {
                    sb.append('?');
                    first = false;
                } else {
                    sb.append("&amp;");
                }
                sb.append(urlEncode(value.getKey()));
                sb.append('=');
                sb.append(urlEncode(value.getValue()));

            }
            return sb.toString();
        }

    }


    private void print(PrintWriter out, XSTypeDefinition definition) {
        out.println(definition);
        if (definition instanceof XSComplexTypeDefinition) {
            XSComplexTypeDefinition complex = (XSComplexTypeDefinition) definition;

            final XSObjectList attributes = complex.getAttributeUses();
            out.println("<ul>");
            for (int i = 0; i < attributes.getLength(); i++) {
                out.println("<li>");
                XSAttributeUse attribute = (XSAttributeUse) attributes.item(i);
                final XSAttributeDeclaration decl = attribute.getAttrDeclaration();
                out.println(decl.getNamespace());
                out.println(decl.getName());
                out.println("</li>");
            }
            out.println("</ul>");

            final XSTerm particle = complex.getParticle().getTerm();
            if (particle instanceof XSModelGroup) {
                out.println("<ol>");
                final XSObjectList particles = ((XSModelGroup) particle).getParticles();
                for (int i = 0; i < particles.getLength(); i++) {
                    final XSObject item = particles.item(i);
                    out.println("<li>");
                    out.println(item);
                    out.println("</li>");
                }
                out.println("</ol>");
            }
        } else {
            out.format("<b>Unknown type %s</b>", definition.getClass());
        }
    }

}