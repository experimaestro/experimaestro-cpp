/**
 * 
 */
package bpiwowar.expmanager.server;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.webserver.XmlRpcServlet;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

import bpiwowar.argparser.ArgParseException;
import bpiwowar.argparser.ArgParser;
import bpiwowar.argparser.Argument;
import bpiwowar.argparser.IllegalArgumentValue;
import bpiwowar.argparser.InvalidHolderException;
import bpiwowar.argparser.checkers.IOChecker;
import bpiwowar.expmanager.Main;
import bpiwowar.expmanager.rsrc.CommandLineTask;
import bpiwowar.expmanager.rsrc.DependencyType;
import bpiwowar.expmanager.rsrc.Resource;
import bpiwowar.expmanager.rsrc.SimpleData;
import bpiwowar.expmanager.rsrc.TaskManager;
import bpiwowar.expmanager.rsrc.SimpleData.Mode;
import bpiwowar.log.Logger;

/**
 * The server displays information about the tasks and responds to XML RPC tasks
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class ServerTask {
	final static private Logger logger = Logger.getLogger();

	@Argument(name = "xmlrpc", help = "XML RPC configuration file")
	File xmlrpcConfigFile;

	@Argument(name = "base", help = "Base directory for the task manager", required = true, checkers = IOChecker.ValidDirectory.class)
	File taskmanagerDirectory;

	@Argument(name = "nb-threads", help = "Number of threads")
	int nbThreads = 10;

	/**
	 * Initialise the server task
	 * 
	 * @param options
	 * @param args
	 * @throws InvalidHolderException
	 * @throws ArgParseException
	 * @throws IllegalArgumentValue
	 */
	public ServerTask(Main options, String[] args)
			throws InvalidHolderException, ArgParseException,
			IllegalArgumentValue {
		ArgParser argParser = new ArgParser("[options]");
		argParser.addOptions(this);
		argParser.matchAllArgs(args);
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
		private TaskManager taskManager;

		void setTaskServer(TaskManager taskManager) {
			this.taskManager = taskManager;
		}

		public boolean runCommand(String name, int priority, Object[] command,
				Object[] depends, Object[] readLocks, Object[] writeLocks) {
			logger.info(
					"Running command %s [%s] (priority %d); read=%s, write=%s",
					name, Arrays.toString(command), priority, Arrays
							.toString(readLocks), Arrays.toString(writeLocks));

			String[] commandArgs = new String[command.length];
			for (int i = command.length; --i >= 0;)
				commandArgs[i] = command[i].toString();

			CommandLineTask job = new CommandLineTask(taskManager, name,
					commandArgs);

			// We have to wait for read lock resources to be generated
			for (Object readLock : readLocks) {
				Resource resource = taskManager.getResource((String) readLock);
				if (resource == null)
					throw new RuntimeException("Resource " + readLock
							+ " was not found");
				job.addDependency(resource, DependencyType.READ_ACCESS);
			}

			for (Object writeLock : writeLocks) {
				final String id = (String) writeLock;
				Resource resource = taskManager.getResource(id);
				if (resource == null) {
					resource = new SimpleData(taskManager, id,
							Mode.EXCLUSIVE_WRITE);
					resource.register(job);
				}
				job.addDependency(resource, DependencyType.WRITE_ACCESS);
			}

			taskManager.add(job);
			return true;
		}
	}

	/**
	 * Server thread
	 */
	public void run() throws Throwable {
		logger.info("Starting server");
		Server server = new Server(8080);

		final TaskManager taskManager = new TaskManager(taskmanagerDirectory,
				nbThreads);

		// --- Set the XML RPC

		Context xmlrpcContext = new Context(server, "/xmlrpc");
		final XmlRpcServlet xmlRpcServlet = new XmlRpcServlet() {
			private static final long serialVersionUID = 1L;

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
												.setTaskServer(taskManager);
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
		};

		xmlRpcServlet.init(new ServletConfig() {
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

		});

		final ServletHolder servletHolder = new ServletHolder(xmlRpcServlet);
		xmlrpcContext.addServlet(servletHolder, "/");

		// --- Adding a general purpose servlet
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
			out.println("session id is " + request.getSession(true).getId());
		}
	}

}