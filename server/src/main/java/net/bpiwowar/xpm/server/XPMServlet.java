package net.bpiwowar.xpm.server;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
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

import org.apache.commons.lang.StringEscapeUtils;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public abstract class XPMServlet extends HttpServlet {
    protected static final String ENCODING = "UTF-8";
    private static final long serialVersionUID = 1L;
    private final ServerSettings serverSettings;

    public XPMServlet(ServerSettings serverSettings) {
        this.serverSettings = serverSettings;
    }

    public static String urlEncode(String text) {
        try {
            return URLEncoder.encode(text, ENCODING);
        } catch (UnsupportedEncodingException e) {
            return "error-while-encoding";
        }
    }

    public static String escapeHtml(String text) {
        return StringEscapeUtils.escapeHtml(text);
    }


    /**
     * Outputs the HTML response header and returns the output stream
     *
     * @param response
     * @return
     * @throws IOException
     */
    protected PrintWriter startHTMLResponse(HttpServletResponse response)
            throws IOException {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        final PrintWriter out = response.getWriter();
        return out;
    }

    void header(PrintWriter out, String title) {
        out.format("<html><head><title>XPM@%s - %s</title>", escapeHtml(serverSettings.name), escapeHtml(title));

        out.format("<link type=\"text/css\" href=\"/css/%s/jquery-ui-1.10.2.custom.min.css\" rel=\"stylesheet\"></link>%n",
                serverSettings.style.toString().toLowerCase()
        );

        out.format("<link type=\"text/css\" href=\"/js/jstree/themes/default/style.min.css\" rel=\"stylesheet\"></link>%n");

        out.format("<link rel=\"stylesheet\" type=\"text/css\" charset=\"utf-8\" media=\"all\" href=\"/css/style.css\">\n");
        out.format("<script type=\"text/javascript\" src=\"/js/jquery-1.9.1.min.js\"></script>\n");
        out.format("<script type=\"text/javascript\" src=\"/js/jquery-ui-1.10.2.custom.min.js\"></script>\n");
        out.format("<script type=\"text/javascript\" src=\"/js/jquery.jsonrpc.js\"></script>\n");
        out.format("<script type=\"text/javascript\" src=\"/js/jstree/jstree.min.js\"></script>\n");

        out.format("<script type=\"text/javascript\" src=\"/js/jquery.ba-hashchange.min.js\"></script>\n");
        out.format("<script type=\"text/javascript\" src='/js/noty/jquery.noty.js'></script>%n");
        out.format("<script type=\"text/javascript\" src='/js/noty/layouts/top.js'></script>%n");
        out.format("<script type=\"text/javascript\" src='/js/noty/themes/default.js'></script>%n");

        out.format("<script type=\"text/javascript\" src=\"/js/xpm.js\"></script>\n");
        out.format("</head>%n");
        out.format("<body>%n");
        out.format("<div id=\"header\"><div class='title'>Experimaestro - %s</div>", serverSettings.name);
        out.format("<div class='links'><a href=\"/status\">Status</a> <a href='/tasks'>Tasks</a> <a href='/jshelp'>JS Help</a></div></div>");

    }

    public void show404(HttpServletResponse response, String format, Object... objects) throws IOException {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        PrintWriter writer = response.getWriter();
        header(writer, "Page not found");
        writer.format("<div class=\"error\">%s</div>", escapeHtml(String.format(format, objects)));
    }

    public void error404(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        final PrintWriter out = response.getWriter();
        header(out, "Error");
        out.println("<h1>Page not found</h1>");
        out.format("<p>This URI was not found: %s</p>", request.getRequestURI());
        out.println("</body></html>");
    }
}