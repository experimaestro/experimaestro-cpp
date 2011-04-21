package sf.net.experimaestro.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

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