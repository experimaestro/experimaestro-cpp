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

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public abstract class XPMServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected static final String ENCODING = "UTF-8";

	public XPMServlet() {
	}

	public static String urlEncode(String text) {
		try {
			return URLEncoder.encode(text, ENCODING);
		} catch (UnsupportedEncodingException e) {
			return "error-while-encoding";
		}
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

}