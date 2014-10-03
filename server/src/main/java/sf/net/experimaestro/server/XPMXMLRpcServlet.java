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

import org.apache.commons.collections.iterators.IteratorEnumeration;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.metadata.Util;
import org.apache.xmlrpc.metadata.XmlRpcSystemImpl;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.webserver.XmlRpcServlet;
import org.eclipse.jetty.server.Server;
import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.arrays.ListAdaptator;
import sf.net.experimaestro.utils.log.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * The XML-RPC servlet for experimaestro
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public final class XPMXMLRpcServlet extends XmlRpcServlet {
    static private final Logger LOGGER = Logger.getLogger();

    private final Repository repository;
    private final Scheduler scheduler;
    private Server server;
    private static final long serialVersionUID = 1L;

    static public final class Config implements ServletConfig {
        public static final String ENABLED_FOR_EXTENSIONS = "enabledForExtensions";
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

        String initParameters[] = {ENABLED_FOR_EXTENSIONS};

        public String getInitParameter(String arg) {
            switch (arg) {
                case ENABLED_FOR_EXTENSIONS:
                    return "true";

            }
            return null;
        }


        public Enumeration<String> getInitParameterNames() {
            return new IteratorEnumeration(ListAdaptator.create(initParameters).iterator());
        }
    }


    /**
     * Initialise the servlet
     *
     * @param repository The main repository
     * @param scheduler  The job scheduler
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
        PropertyHandlerMapping mapping = new PropertyHandlerMapping() {
            @Override
            protected String getMethodHelp(Class pClass, Method[] pMethods) {
                final List<String> result = new ArrayList<>();

                for (Method method : pMethods) {
                    final RPCMethod annotation = method.getAnnotation(RPCMethod.class);
                    if (annotation == null)
                        result.add(Util.getMethodHelp(pClass, method));
                    else
                        result.add(annotation.help());
                }

                switch (result.size()) {
                    case 0:
                        return null;
                    case 1:
                        return (String) result.get(0);
                    default:
                        StringBuffer sb = new StringBuffer();
                        for (int i = 0; i < result.size(); i++) {
                            sb.append(i + 1);
                            sb.append(": ");
                            sb.append(result.get(i));
                            sb.append("\n");
                        }
                        return sb.toString();
                }
            }

        };

        RequestProcessorFactoryFactory factory = new RequestProcessorFactoryFactory() {
            public RequestProcessorFactory getRequestProcessorFactory(
                    @SuppressWarnings("rawtypes") final Class pClass) throws XmlRpcException {
                return new RequestProcessorFactory() {

                    // TODO: Try to exploit streaming
                    public Object getRequestProcessor(XmlRpcRequest pRequest)
                            throws XmlRpcException {
                        try {
                            LOGGER.debug("XML-RPC request is class is %s", pRequest == null ? "null" : pRequest.getClass().getCanonicalName());
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

        // Adds introspection (system....)
        XmlRpcSystemImpl.addSystemHandler(mapping);
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