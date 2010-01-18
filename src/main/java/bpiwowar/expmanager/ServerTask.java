/**
 * 
 */
package bpiwowar.expmanager;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

import bpiwowar.argparser.ArgParseException;
import bpiwowar.argparser.ArgParser;
import bpiwowar.argparser.IllegalArgumentValue;
import bpiwowar.argparser.InvalidHolderException;
import bpiwowar.argparser.StringScanException;
import bpiwowar.log.Logger;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
class ServerTask  {
	final static private Logger logger = Logger.getLogger();
	private final Main options;

	public ServerTask(Main options, String[] args)
			throws InvalidHolderException, ArgParseException,
			StringScanException, IllegalArgumentValue {
		this.options = options;
		ArgParser argParser = new ArgParser("[options]");
		argParser.addOptions(this);
		argParser.matchAllArgs(args);
	}

	public void run() throws Throwable {
		logger.info("Starting server");
		Server server = new Server(8080);
		Context context = new Context(server, "/", Context.SESSIONS);
		context.addServlet(new ServletHolder(new HelloServlet()), "/*");

		server.start();
		server.join();
	}

	public static class HelloServlet extends HttpServlet {
		private static final long serialVersionUID = 1L;

		protected void doGet(HttpServletRequest request,
				HttpServletResponse response) throws ServletException,
				IOException {
			response.setContentType("text/html");
			response.setStatus(HttpServletResponse.SC_OK);
			final PrintWriter out = response.getWriter();
			out.println("<h1>Hello SimpleServlet</h1>");
			out.format("<p>%s</p>", request.getRequestURI());
			out.println(
					"session=" + request.getSession(true).getId());
		}
	}

}