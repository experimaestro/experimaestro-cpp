package sf.net.experimaestro.server;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.webserver.XmlRpcServlet;

import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.scheduler.Scheduler;

/**
 * The XML-RPC servlet
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
/**
 * The XML-RPC servlet for experimaestro
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 *
 */
public final class XPMXMLRpcServlet extends XmlRpcServlet {
	private final Repository repository;
	private final Scheduler taskManager;
	private static final long serialVersionUID = 1L;
	
	static public final class Config implements ServletConfig {
		private final XmlRpcServlet xmlRpcServlet;

		public Config(XmlRpcServlet xmlRpcServlet) {
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

		public Enumeration<?> getInitParameterNames() {
			return new Enumeration<Object>() {
				public boolean hasMoreElements() {
					return false;
				}

				public Object nextElement() {
					throw new NoSuchElementException();
				}
			};
		}
	}
	
	

	public XPMXMLRpcServlet(Repository repository,
			Scheduler taskManager) {
		this.repository = repository;
		this.taskManager = taskManager;
	}
	

	@Override
	protected PropertyHandlerMapping newPropertyHandlerMapping(URL url)
			throws IOException, XmlRpcException {
		PropertyHandlerMapping mapping = new PropertyHandlerMapping();

		RequestProcessorFactoryFactory factory = new RequestProcessorFactoryFactory() {
			public RequestProcessorFactory getRequestProcessorFactory(
					@SuppressWarnings("rawtypes") final Class pClass) throws XmlRpcException {
				return new RequestProcessorFactory() {

					public Object getRequestProcessor(XmlRpcRequest pRequest)
							throws XmlRpcException {
						try {
							Object object = pClass.newInstance();
							if (object instanceof RPCTaskManager) {
								((RPCTaskManager) object).setTaskServer(
										taskManager, repository);
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