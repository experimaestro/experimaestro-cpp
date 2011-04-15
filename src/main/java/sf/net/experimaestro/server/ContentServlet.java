package sf.net.experimaestro.server;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.VFS;

import sf.net.experimaestro.utils.log.Logger;

public class ContentServlet extends HttpServlet {
	final static private Logger LOGGER = Logger.getLogger();
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		URL url = ContentServlet.class.getResource(format("/web%s",
				request.getRequestURI()));

		if (url != null) {
			FileSystemManager fsManager = VFS.getManager();
			FileObject file = fsManager.resolveFile(url.toExternalForm());
			if (file.getType() == FileType.FOLDER) {
				response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
				response.setHeader("Location",
						format("%sindex.html", request.getRequestURI()));
				return;
			}

			response.setContentType("text/html");
			response.setStatus(HttpServletResponse.SC_OK);
			final PrintWriter out = response.getWriter();
			Reader in = new InputStreamReader(url.openStream());
			char[] buffer = new char[8192];
			int read;
			while ((read = in.read(buffer)) > 0) {
				out.write(buffer, 0, read);
			}
			out.flush();
			in.close();
			return;
		}

		// Not found
		error404(request, response);

	}

	public static void error404(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		final PrintWriter out = response.getWriter();
		out.println("<html><head><title>Error</title></head><body>");
		out.println("<h1>Page not found</h1>");
		out.format("<p>This URI was not found: %s</p>", request.getRequestURI());
		out.println("</body></html>");
	}
}