/**
 * 
 */
package sf.net.experimaestro.tasks;

import java.io.File;

import org.apache.xmlrpc.webserver.XmlRpcServlet;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.security.Constraint;
import org.mortbay.jetty.security.ConstraintMapping;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.SecurityHandler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

import sf.net.experimaestro.manager.TaskRepository;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.server.ContentServlet;
import sf.net.experimaestro.server.StatusServlet;
import sf.net.experimaestro.server.TasksServlet;
import sf.net.experimaestro.server.XPMXMLRpcServlet;
import sf.net.experimaestro.tasks.config.XMLRPCClientConfig;
import sf.net.experimaestro.utils.Output;
import sf.net.experimaestro.utils.log.Logger;
import bpiwowar.argparser.Argument;
import bpiwowar.argparser.ArgumentClass;
import bpiwowar.argparser.checkers.IOChecker;
import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;

/**
 * The server displays information about the tasks and responds to XML RPC tasks
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@TaskDescription(name = "server", project = { "xpmanager" })
public class ServerTask extends AbstractTask {
	final static Logger LOGGER = Logger.getLogger();

	@ArgumentClass(prefix = "xmlrpc", help = "Configuration file for the XML RPC call", required = true)
	XMLRPCClientConfig xmlrpcClientConfig;

	@Argument(name = "base", help = "Base directory for the task manager", required = true, checkers = IOChecker.ValidDirectory.class)
	File taskmanagerDirectory;

	@Argument(name = "nb-threads", help = "Number of threads")
	int nbThreads = 10;


	/**
	 * Server thread
	 */
	public int execute() throws Throwable {

		int port = Integer.parseInt(xmlrpcClientConfig.getProperty("port",
				"8080"));
		LOGGER.info("Starting server on port %d", port);

		// --- Set up the task manager
		final Scheduler taskManager = new Scheduler(taskmanagerDirectory,
				nbThreads);

		final TaskRepository repository = new TaskRepository();

		// Set up the server
		Server server = new Server(port);


		Context context = new Context(server, "/");

		// -- Security
		// From
		// http://docs.codehaus.org/display/JETTY/How+to+Configure+Security+with+Embedded+Jetty
		Constraint constraint = new Constraint();
		constraint.setName(Constraint.__BASIC_AUTH);
		constraint.setRoles(new String[] { "user" });
		constraint.setAuthenticate(true);

		ConstraintMapping cm = new ConstraintMapping();
		cm.setConstraint(constraint);
		cm.setPathSpec("/*");

		SecurityHandler sh = new SecurityHandler();
		String passwordProperty = xmlrpcClientConfig.getProperty("passwords",
				null);
		if (passwordProperty == null)
			passwordProperty = new File(System.getProperty("user.home"),
					".experimaestro.passwords").getAbsolutePath();
		File passwordFile = new File(passwordProperty);

		sh.setUserRealm(new HashUserRealm("xpm-realm", passwordFile
				.getAbsolutePath()));
		sh.setConstraintMappings(new ConstraintMapping[] { cm });
		context.addHandler(sh);

		// --- Add the XML RPC servlet

		final XmlRpcServlet xmlRpcServlet = new XPMXMLRpcServlet(repository,
				taskManager);
		xmlRpcServlet.init(new XPMXMLRpcServlet.Config(xmlRpcServlet));

		final ServletHolder servletHolder = new ServletHolder(xmlRpcServlet);
		context.addServlet(servletHolder, "/xmlrpc");

		// --- Add the status servlet

		context.addServlet(new ServletHolder(new StatusServlet(taskManager)),
				"/status/*");

		
		// --- Add the status servlet

		context.addServlet(new ServletHolder(new TasksServlet(repository, taskManager)),
				"/tasks/*");

		
		// --- Add the default servlet

		context.addServlet(new ServletHolder(new ContentServlet()), "/*");

		// final URL warUrl =
		// this.getClass().getClassLoader().getResource("web");
		// final String warUrlString = warUrl.toExternalForm();
		// server.setHandler(new WebAppContext(warUrlString, "/"));

		// --- start the server

		server.start();
		server.join();

		return 0;
	}

	


}
