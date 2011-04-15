/**
 * 
 */
package sf.net.experimaestro.tasks;

import static java.lang.String.format;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Level;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.webserver.XmlRpcServlet;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.security.Constraint;
import org.mortbay.jetty.security.ConstraintMapping;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.SecurityHandler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrappedException;

import sf.net.experimaestro.locks.LockType;
import sf.net.experimaestro.log.Logger;
import sf.net.experimaestro.manager.TaskRepository;
import sf.net.experimaestro.manager.XPMObject;
import sf.net.experimaestro.rsrc.CommandLineTask;
import sf.net.experimaestro.rsrc.LockMode;
import sf.net.experimaestro.rsrc.Resource;
import sf.net.experimaestro.rsrc.SimpleData;
import sf.net.experimaestro.rsrc.Task;
import sf.net.experimaestro.rsrc.TaskManager;
import sf.net.experimaestro.tasks.config.XMLRPCClientConfig;
import sf.net.experimaestro.utils.Output;
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
	final static private Logger LOGGER = Logger.getLogger();

	@ArgumentClass(prefix = "xmlrpc", help = "Configuration file for the XML RPC call", required = true)
	XMLRPCClientConfig xmlrpcClientConfig;

	@Argument(name = "base", help = "Base directory for the task manager", required = true, checkers = IOChecker.ValidDirectory.class)
	File taskmanagerDirectory;

	@Argument(name = "nb-threads", help = "Number of threads")
	int nbThreads = 10;

	private final class XPMServletConfig implements ServletConfig {
		private final XmlRpcServlet xmlRpcServlet;

		private XPMServletConfig(XmlRpcServlet xmlRpcServlet) {
			this.xmlRpcServlet = xmlRpcServlet;
		}

		public String getServletName() {
			return xmlRpcServlet.getClass().getName();
		}

		public ServletContext getServletContext() {
			throw new IllegalStateException("Context not available");
		}

		public String getInitParameter(String pArg0) {
			return null;
		}

		@SuppressWarnings("unchecked")
		public Enumeration<?> getInitParameterNames() {
			return new Enumeration() {
				public boolean hasMoreElements() {
					return false;
				}

				public Object nextElement() {
					throw new NoSuchElementException();
				}
			};
		}
	}

	private final class XPMXMLRpcServlet extends XmlRpcServlet {
		private final TaskRepository repository;
		private final TaskManager taskManager;
		private static final long serialVersionUID = 1L;

		private XPMXMLRpcServlet(TaskRepository repository,
				TaskManager taskManager) {
			this.repository = repository;
			this.taskManager = taskManager;
		}

		@Override
		protected PropertyHandlerMapping newPropertyHandlerMapping(URL url)
				throws IOException, XmlRpcException {
			PropertyHandlerMapping mapping = new PropertyHandlerMapping();
			// mapping.setAuthenticationHandler(authenticationHandler);

			RequestProcessorFactoryFactory factory = new RequestProcessorFactoryFactory() {
				public RequestProcessorFactory getRequestProcessorFactory(
						final Class pClass) throws XmlRpcException {
					return new RequestProcessorFactory() {

						public Object getRequestProcessor(
								XmlRpcRequest pRequest)
								throws XmlRpcException {
							try {
								Object object = pClass.newInstance();
								if (object instanceof RPCTaskManager) {
									((RPCTaskManager) object)
											.setTaskServer(taskManager,
													repository);
								}
								return object;
							} catch (InstantiationException e) {
								throw new RuntimeException(e);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						}
					};
				}
			};

			mapping.setRequestProcessorFactoryFactory(factory);
			mapping.addHandler("Calculator", Calculator.class);
			mapping.addHandler("TaskManager", RPCTaskManager.class);

			return mapping;
		}

		@Override
		protected XmlRpcHandlerMapping newXmlRpcHandlerMapping() {
			try {
				return newPropertyHandlerMapping(null);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Just for debug purposes, we provide a Calculator with two methods (add
	 * and substract)
	 * 
	 * @author B. Piwowarski <benjamin@bpiwowar.net>
	 */
	static public class Calculator {
		public int add(int i1, int i2) {
			return i1 + i2;
		}

		public int subtract(int i1, int i2) {
			return i1 - i2;
		}
	}

	/**
	 * Our RPC handler for task manager
	 * 
	 * @author B. Piwowarski <benjamin@bpiwowar.net>
	 */
	static public class RPCTaskManager {

		/**
		 * The task manager
		 */
		private TaskManager taskManager;

		/**
		 * Repository
		 */
		TaskRepository repository;

		void setTaskServer(TaskManager taskManager, TaskRepository repository) {
			this.taskManager = taskManager;
			this.repository = repository;
		}

		/**
		 * Add a data resource
		 * 
		 * @param id
		 *            The data ID
		 * @param mode
		 *            The locking mode
		 * @param exists
		 * @return
		 */
		public boolean addData(String id, String mode, boolean exists) {
			LOGGER.info("Addind data %s [%s/%b]", id, mode, exists);
			taskManager.add(new SimpleData(taskManager, id, LockMode
					.valueOf(mode), exists));
			return true;
		}

		/**
		 * A class that is used to control the environment in scripts
		 * 
		 * @author B. Piwowarski <benjamin@bpiwowar.net>
		 */
		static public class JSGetEnv {
			private final Map<String, String> environment;

			public JSGetEnv(Map<String, String> environment) {
				this.environment = environment;
			}

			public String get(String key) {
				return environment.get(key);
			}

			public String get(String key, String defaultValue) {
				String value = environment.get(key);
				if (value == null)
					return defaultValue;
				return value;
			}

		}

		/**
		 * Run a javascript script (either the file or a string)
		 * 
		 * This version is called from python scripts where maps would be
		 * marshalled into a string. Instead, we get a list that we transform
		 * into a map.
		 */
		public ArrayList<Object> runJSScript(boolean isFile, String content,
				Object[] envArray) {
			Map<String, String> environment = arrayToMap(envArray);
			return runJSScript(isFile, content, environment);
		}

		/**
		 * Run a javascript script (either the file or a string)
		 */
		public ArrayList<Object> runJSScript(boolean isFile, String content,
				Map<String, String> environment) {
			int error = 0;
			String errorMsg = "";
			XPMObject jsXPM = null;

			// Creates and enters a Context. The Context stores information
			// about the execution environment of a script.
			try {
				org.mozilla.javascript.Context cx = org.mozilla.javascript.Context
						.enter();

				// Initialize the standard objects (Object, Function, etc.)
				// This must be done before scripts can be executed. Returns
				// a scope object that we use in later calls.
				Scriptable scope = cx.initStandardObjects();

				LOGGER.info("Environment is: %s", Output.toString(", ",
						environment.entrySet(),
						new Output.Formatter<Map.Entry<String, String>>() {
							@Override
							public String format(Entry<String, String> t) {
								return String.format("%s: %s", t.getKey(),
										t.getValue());
							}
						}));

				if (isFile) {
					environment.put(XPMObject.ENV_SCRIPTPATH, content);
				}

				ScriptableObject.defineProperty(scope, "env", new JSGetEnv(
						environment), 0);
				jsXPM = new XPMObject(cx, environment, scope, repository,
						taskManager);
				XPMObject.getLog().clear();

				ScriptableObject.defineProperty(scope, "xpm", jsXPM, 0);

				final Object result;
				if (isFile)
					result = cx.evaluateReader(scope, new FileReader(content),
							content, 1, null);
				else
					result = cx
							.evaluateString(scope, content, "stdin", 1, null);

				if (result != null)
					LOGGER.info(result.toString());
				else
					LOGGER.info("Null result");

				// Object object = scope.get("Task", null);
				// if (object instanceof NativeFunction) {
				// org.mozilla.javascript.Context cx2 =
				// org.mozilla.javascript.Context
				// .enter();
				// ((NativeFunction) object).call(cx2, scope, scope, null);
				// org.mozilla.javascript.Context.exit();
				// }

			} catch (WrappedException e) {
				LOGGER.printException(Level.INFO, e.getCause());
				error = 2;
				errorMsg = e.getCause().toString() + "\n[in] " + e.toString();
				errorMsg += "\n" + e.getScriptStackTrace();
			} catch (JavaScriptException e) {
				LOGGER.printException(Level.INFO, e);
				error = 3;
				errorMsg = e.toString();
				errorMsg += "\n" + e.getScriptStackTrace();
			} catch (Exception e) {
				LOGGER.printException(Level.INFO, e);
				error = 1;
				errorMsg = e.toString();
			} finally {
				// Exit context
				org.mozilla.javascript.Context.exit();
			}

			ArrayList<Object> list = new ArrayList<Object>();
			list.add(error);
			list.add(errorMsg);
			if (jsXPM != null) {
				list.add(XPMObject.getLog());
			}
			XPMObject.resetLog();
			return list;
		}

		/**
		 * Add a command line job
		 */
		public boolean runCommand(String name, int priority, Object[] command,
				Object[] envArray, String workingDirectory, Object[] depends,
				Object[] readLocks, Object[] writeLocks) {
			Map<String, String> env = arrayToMap(envArray);
			LOGGER.info(
					"Running command %s [%s] (priority %d); read=%s, write=%s; environment={%s}",
					name, Arrays.toString(command), priority,
					Arrays.toString(readLocks), Arrays.toString(writeLocks),
					Output.toString(", ", env.entrySet()));

			String[] commandArgs = new String[command.length];
			for (int i = command.length; --i >= 0;)
				commandArgs[i] = command[i].toString();

			CommandLineTask job = new CommandLineTask(taskManager, name,
					commandArgs, env, new File(workingDirectory));

			// Process locks
			for (Object depend : depends) {
				Resource resource = taskManager.getResource((String) depend);
				if (resource == null)
					throw new RuntimeException("Resource " + depend
							+ " was not found");
				job.addDependency(resource, LockType.GENERATED);
			}

			// We have to wait for read lock resources to be generated
			for (Object readLock : readLocks) {
				Resource resource = taskManager.getResource((String) readLock);
				if (resource == null)
					throw new RuntimeException("Resource " + readLock
							+ " was not found");
				job.addDependency(resource, LockType.READ_ACCESS);
			}

			// Write locks
			for (Object writeLock : writeLocks) {
				final String id = (String) writeLock;
				Resource resource = taskManager.getResource(id);
				if (resource == null) {
					resource = new SimpleData(taskManager, id,
							LockMode.EXCLUSIVE_WRITER, false);
					resource.register(job);
				}
				job.addDependency(resource, LockType.WRITE_ACCESS);
			}

			taskManager.add(job);
			return true;
		}

		/**
		 * Utility function that transforms an array with paired values into a
		 * map
		 * 
		 * @param envArray
		 *            The array, must contain an even number of elements
		 * @return a map
		 */
		private Map<String, String> arrayToMap(Object[] envArray) {
			Map<String, String> env = new TreeMap<String, String>();
			for (Object x : envArray) {
				Object[] o = (Object[]) x;
				if (o.length != 2)
					// FIXME: should be a proper one
					throw new RuntimeException();
				env.put((String) o[0], (String) o[1]);
			}
			return env;
		}
	}

	/**
	 * Server thread
	 */
	public int execute() throws Throwable {

		int port = Integer.parseInt(xmlrpcClientConfig.getProperty("port",
				"8080"));
		LOGGER.info("Starting server on port %d", port);
		Server server = new Server(port);

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

		// --- Set up the task manager
		final TaskManager taskManager = new TaskManager(taskmanagerDirectory,
				nbThreads);

		final TaskRepository repository = new TaskRepository();

		// --- Set the XML RPC

		Context xmlrpcContext = new Context(server, "/xmlrpc");
		xmlrpcContext.addHandler(sh);

		final XmlRpcServlet xmlRpcServlet = new XPMXMLRpcServlet(repository, taskManager);
		xmlRpcServlet.init(new XPMServletConfig(xmlRpcServlet));

		final ServletHolder servletHolder = new ServletHolder(xmlRpcServlet);
		xmlrpcContext.addServlet(servletHolder, "/");

		// --- Adding a security handler
		Context context = new Context(server, "/");

		sh = new SecurityHandler();
		sh.setUserRealm(new HashUserRealm("xpm-realm", passwordFile
				.getAbsolutePath()));
		sh.setConstraintMappings(new ConstraintMapping[] { cm });
		context.addHandler(sh);

		context.addServlet(new ServletHolder(new StatusServlet(taskManager)),
				"/status");

		// --- Finish with the last 

		final URL warUrl = this.getClass().getClassLoader().getResource("web");
		final String warUrlString = warUrl.toExternalForm();
		server.setHandler(new WebAppContext(warUrlString, "/"));

		
		// --- start the server
		
		server.start();
		server.join();

		return 0;
	}

	/**
	 * Gives the current task status
	 * 
	 * @author B. Piwowarski <benjamin@bpiwowar.net>
	 */
	public class StatusServlet extends HttpServlet {
		private static final long serialVersionUID = 1L;
		private final TaskManager manager;

		public StatusServlet(TaskManager manager) {
			this.manager = manager;
		}

		protected void doGet(HttpServletRequest request,
				HttpServletResponse response) throws ServletException,
				IOException {
			response.setContentType("text/html");
			response.setStatus(HttpServletResponse.SC_OK);
			final PrintWriter out = response.getWriter();

			out.println("<h1>Waiting tasks</h1>");
			out.println("<ul>");
			for (Task task : manager.tasks()) {
				out.format("<li>%s</li>", task);
			}
			out.println("</ul>");

			out.println("<h1>List of resources (generated)</h1>");
			out.println("<ul>");
			for (Resource resource : manager.resources()) {
				if (resource.isGenerated())
					out.format("<li>[%s] %s</li>", resource.getClass(),
							resource);
			}
			out.println("</ul>");

			out.println("<h1>List of resources (not generated)</h1>");
			out.println("<ul>");
			for (Resource resource : manager.resources()) {
				if (!resource.isGenerated())
					out.format("<li>[%s] %s</li>", resource.getClass(),
							resource);
			}
			out.println("</ul>");
		}
	}

	public static class GenericServlet extends HttpServlet {
		private static final long serialVersionUID = 1L;

		protected void doGet(HttpServletRequest request,
				HttpServletResponse response) throws ServletException,
				IOException {
			URL resource = GenericServlet.class.getResource(format("%s",request
					.getRequestURI()));
			
			if (resource == null) {
				response.setContentType("text/html");
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				final PrintWriter out = response.getWriter();
				out.println("<html><head><title>Error</title></head><body>");
				out.println("<h1>Page not found</h1>");
				out.format("<p>This URI was not found:<br/>%s</p>",
						request.getRequestURI());
				out.println("</body></html>");
			} else {
			}
		}
	}

}