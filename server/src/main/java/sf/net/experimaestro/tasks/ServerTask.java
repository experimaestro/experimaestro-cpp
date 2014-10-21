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

/**
 *
 */
package sf.net.experimaestro.tasks;

import bpiwowar.argparser.ArgumentClass;
import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.xmlrpc.webserver.XmlRpcServlet;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.util.thread.ThreadPool;
import sf.net.experimaestro.connectors.XPMConnector;
import sf.net.experimaestro.manager.Repositories;
import sf.net.experimaestro.scheduler.ResourceLocator;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.server.*;
import sf.net.experimaestro.utils.log.Logger;

import java.io.File;
import java.net.URL;
import java.util.Iterator;

/**
 * The server displays information about the tasks and responds to XML RPC tasks
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@TaskDescription(name = "server", project = {"xpmanager"})
public class ServerTask extends AbstractTask {
    final static Logger LOGGER = Logger.getLogger();
    public static final String KEY_SERVER_SOCKET = "server.socket";
    public static final String XPM_REALM = "xpm-realm";

    @ArgumentClass(prefix = "conf", help = "Configuration file for the XML RPC call")
    HierarchicalINIConfiguration configuration;

    /**
     * Server thread
     */
    public int execute() throws Throwable {
        if (configuration == null || configuration.getFile() == null) {
            final File file = new File(new File(System.getProperty("user.home"), ".experimaestro"), "settings.ini");
            LOGGER.info("Using the default configuration file " + file);
            configuration = new HierarchicalINIConfiguration(file);
        }
        LOGGER.info("Reading configuration from " + configuration.getFileName());

        // --- Get the server settings
        ServerSettings serverSettings = new ServerSettings(configuration.subset("server"));

        // --- Get the port
        int port = configuration.getInt("server.port", 8080);
        LOGGER.info("Starting server on port %d", port);

        // --- Set up the task manager
        final String property = configuration.getString("server.database");
        if (property == null)
            throw new IllegalArgumentException("No 'database' in 'server' section of the configuration file");

        File taskmanagerDirectory = new File(property);
        final Scheduler scheduler = new Scheduler(taskmanagerDirectory);

        // Main repository
        final Repositories repositories = new Repositories(new ResourceLocator(XPMConnector.getInstance(), ""));

        Server webServer = new Server();

        // TCP-IP socket
        ServerConnector connector=new ServerConnector(webServer);
        connector.setPort(port);
        webServer.addConnector(connector);

        // Unix domain socket
        if (configuration.containsKey(KEY_SERVER_SOCKET)) {
            // TODO: move this to the target class
            String libraryPath = System.getProperty("org.newsclub.net.unix.library.path");
            if (libraryPath == null) {
                URL url = ServerTask.class.getProtectionDomain().getCodeSource().getLocation();
                File file = new File(url.toURI());
                while (file != null && !new File(file, "native-libs").exists()) {
                    file = file.getParentFile();
                }
                if (file == null)
                    throw new UnsatisfiedLinkError("Cannot find the native-libs directory");
                file = new File(file, "native-libs");

                LOGGER.info("Using path for junixsocket library [%s]", file);
                System.setProperty("org.newsclub.net.unix.library.path", file.getAbsolutePath());
            }

            String socketSpec = configuration.getString(KEY_SERVER_SOCKET);
            UnixSocketConnector unixSocketConnector = new UnixSocketConnector(webServer);
            unixSocketConnector.setSocketFile(new File(socketSpec));
            webServer.addConnector(unixSocketConnector);
        }


        ServletContextHandler context = new ServletContextHandler(webServer, "/");
        ConstraintSecurityHandler csh = getSecurityHandler();


        context.setSecurityHandler(csh);


        // --- Add the JSON RPC servlet

        final JsonRPCServlet jsonRpcServlet = new JsonRPCServlet(webServer, scheduler, repositories);
        JsonRPCMethods.initMethods();
        final ServletHolder jsonServletHolder = new ServletHolder(jsonRpcServlet);
        context.addServlet(jsonServletHolder, "/json-rpc");


        // --- Add the web socket servlet
        final XPMWebSocketServlet webSocketServlet = new XPMWebSocketServlet(webServer, scheduler, repositories);
//        webSocketServlet.init(new XPMXMLRpcServlet.Config(xmlRpcServlet));

        final ServletHolder webSocketServletHolder = new ServletHolder(webSocketServlet);
        context.addServlet(webSocketServletHolder, "/web-socket");


        // --- Add the status servlet

        context.addServlet(new ServletHolder(new StatusServlet(serverSettings, scheduler)), "/status/*");

        // --- Add the status servlet

        context.addServlet(new ServletHolder(new TasksServlet(serverSettings,repositories,
                scheduler)), "/tasks/*");


        // --- Add the JS Help servlet

        context.addServlet(new ServletHolder(new JSHelpServlet(serverSettings)), "/jshelp/*");


        // --- Add the default servlet

        context.addServlet(new ServletHolder(new ContentServlet(serverSettings)), "/*");


        // final URL warUrl =
        // this.getClass().getClassLoader().getResource("web");
        // final String warUrlString = warUrl.toExternalForm();
        // server.setHandler(new WebAppContext(warUrlString, "/"));

        // --- start the server

        webServer.start();
        ThreadPool threadPool = webServer.getThreadPool();



        // --- Wait for servers to close
        threadPool.join();


        LOGGER.info("Servers are stopped. Clean exit!");

        return 0;
    }

    private ConstraintSecurityHandler getSecurityHandler() {
        // -- Security
        // From
        // http://docs.codehaus.org/display/JETTY/How+to+Configure+Security+with+Embedded+Jetty

        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);
        constraint.setRoles(new String[]{"user"});
        constraint.setAuthenticate(true);

        ConstraintMapping cm = new ConstraintMapping();
        cm.setConstraint(constraint);
        cm.setPathSpec("/*");

        String passwordProperty = configuration.getString("passwords");
        final HashLoginService loginService;
        if (passwordProperty != null) {
            File passwordFile = new File(passwordProperty);
            loginService = new HashLoginService(XPM_REALM, passwordFile
                    .getAbsolutePath());
        } else
            loginService = new HashLoginService(XPM_REALM);

        // Read passwords
        final SubnodeConfiguration passwords = configuration.getSection("passwords");
        final Iterator keys = passwords.getKeys();
        while (keys.hasNext()) {
            String user = (String) keys.next();
            final String[] fields = passwords.getString(user).split("\\s*,\\s*", 2);
            final String[] roles = fields[1].split("\\s*,\\s*");

            LOGGER.info("Adding user %s", user);
            loginService.putUser(user, new Password(fields[0]), roles);
        }

        ConstraintSecurityHandler csh = new ConstraintSecurityHandler();
        csh.setRealmName(XPM_REALM);
        csh.setAuthenticator(new BasicAuthenticator());
        csh.setConstraintMappings(new ConstraintMapping[] {cm});
        csh.addConstraintMapping(cm);
        csh.setLoginService(loginService);
        return csh;
    }


}
