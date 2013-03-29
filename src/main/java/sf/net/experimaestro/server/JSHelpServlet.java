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

import sf.net.experimaestro.manager.js.JSDocumentation;
import sf.net.experimaestro.utils.Documentation;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Help about JavaScript
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class JSHelpServlet extends XPMServlet {
	private static final long serialVersionUID = 1L;

    public JSHelpServlet(ServerSettings serverSettings) {
        super(serverSettings);
    }

    protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		String localPath = request.getRequestURI().substring(
				request.getServletPath().length());

		if (localPath.equals("")) {
			final PrintWriter out = startHTMLResponse(response);
            header(out, "JavaScript Help");

            final Documentation.Printer printer = new Documentation.HTMLPrinter(out);
            JSDocumentation.printJSHelp(printer);
            out.println("</body></html>");
            return;
		}

		// Not found
		error404(request, response);
	}


}