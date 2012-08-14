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

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.webserver.XmlRpcServlet;
import org.mortbay.jetty.Server;
import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.log.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * The XML-RPC servlet for experimaestro
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 *
 */
public final class XPMXMLRpcServlet extends XmlRpcServlet {
    static private final Logger LOGGER = Logger.getLogger();

	private final Repository repository;
	private final Scheduler scheduler;
	private Server server;
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
	
	

	/**
	 * Initialise the servlet
     *
	 * @param repository The main repository
	 * @param scheduler The job scheduler
	 */
	public XPMXMLRpcServlet(Server server, Repository repository,
			Scheduler scheduler) {
		this.repository = repository;
		this.scheduler = scheduler;
		this.server = server;
	}
	

	@Override
	protected PropertyHandlerMapping newPropertyHandlerMapping(URL url)
			throws IOException, XmlRpcException {
		PropertyHandlerMapping mapping = new PropertyHandlerMapping();

		RequestProcessorFactoryFactory factory = new RequestProcessorFactoryFactory() {
			public RequestProcessorFactory getRequestProcessorFactory(
					@SuppressWarnings("rawtypes") final Class pClass) throws XmlRpcException {
				return new RequestProcessorFactory() {

                    // TODO: Try to exploit streaming
					public Object getRequestProcessor(XmlRpcRequest pRequest)
							throws XmlRpcException {
						try {
                            LOGGER.info("XML-RPC request is class is %s", pRequest == null ? "null" : pRequest.getClass().getCanonicalName());
                            // Create a new instance for the class pClass
							Object object = pClass.newInstance();
							if (object instanceof RPCHandler) {
								((RPCHandler) object).setTaskServer(pRequest, server, scheduler, repository);
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
		mapping.addHandler("Server", RPCHandler.class);

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