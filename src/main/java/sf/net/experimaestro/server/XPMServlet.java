package sf.net.experimaestro.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

public abstract class XPMServlet extends HttpServlet {

	protected static final String ENCODING = "UTF-8";

	public XPMServlet() {
	}

	protected String urlEncode(String text)
			throws UnsupportedEncodingException {
				return URLEncoder.encode(text, ENCODING);
			}

	/**
	 * Outputs the HTML response header and returns the output stream
	 * @param response
	 * @return
	 * @throws IOException
	 */
	protected PrintWriter startHTMLResponse(HttpServletResponse response) throws IOException {
		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);
		final PrintWriter out = response.getWriter();
		return out;
	}

}