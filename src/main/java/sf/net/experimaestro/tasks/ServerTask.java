/*
 *
 *  This file is part of experimaestro.
 *  Copyright (c) 2011 B. Piwowarski <benjamin@bpiwowar.net>
 *
 *  experimaestro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  experimaestro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 *
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
import org.mortbay.jetty.Server;
import org.mortbay.jetty.security.*;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.server.ContentServlet;
import sf.net.experimaestro.server.StatusServlet;
import sf.net.experimaestro.server.TasksServlet;
import sf.net.experimaestro.server.XPMXMLRpcServlet;
import sf.net.experimaestro.utils.log.Logger;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;

/**
 * The server displays information about the tasks and responds to XML RPC tasks
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@TaskDescription(name = "server", project = {"xpmanager"})
public class ServerTask extends AbstractTask {
    final static Logger LOGGER = Logger.getLogger();

    @ArgumentClass(prefix = "conf", help = "Configuration file for the XML RPC call")
    HierarchicalINIConfiguration configuration;

//	@Argument(name = "base", help = "Base directory for the task manager", required = true, checkers = IOChecker.ValidDirectory.class)
//	File taskmanagerDirectory;

    private Server server;

    /**
     * Server thread
     */
    public int execute() throws Throwable {
        if (configuration == null || configuration.getFile() == null) {
            final File file = new File(System.getProperty("user.home"), ".experimaestro");
            LOGGER.info("Using the default configuration file " + file);
            configuration = new HierarchicalINIConfiguration(file);
        }
        LOGGER.info("Reading configuration from " + configuration.getFileName());

        // --- Get the port
        int port = configuration.getInt("server.port", 8080);
        LOGGER.info("Starting server on port %d", port);

        // --- Set up the task manager
        final String property = configuration.getString("server.database");
        if (property == null)
            throw new IllegalArgumentException("No 'database' in 'server' section of the configuration file");

        File taskmanagerDirectory = new File(property);
        final Scheduler taskManager = new Scheduler(taskmanagerDirectory);

        final Repository repository = new Repository();

        server = new Server(port);

        Context context = new Context(server, "/");

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

        SecurityHandler sh = new SecurityHandler();
        String passwordProperty = configuration.getString("passwords");
        final HashUserRealm userRealm;
        if (passwordProperty != null) {
            File passwordFile = new File(passwordProperty);
            userRealm = new HashUserRealm("xpm-realm", passwordFile
                    .getAbsolutePath());
        } else
            userRealm = new HashUserRealm("xpm-realm");

        // Read passwords
        final SubnodeConfiguration passwords = configuration.getSection("passwords");
        final Iterator<String> keys = passwords.getKeys();
        while (keys.hasNext()) {
            String user = keys.next();
            final String[] fields = passwords.getString(user).split("\\s*,\\s*");

            LOGGER.info("Adding user %s [%s]", user, Arrays.toString(fields));
            userRealm.put(user, new Password(fields[0]));
            for (int i = 1; i < fields.length; i++)
                userRealm.addUserToRole(user, fields[i]);
        }


        sh.setUserRealm(userRealm);
        sh.setConstraintMappings(new ConstraintMapping[]{cm});
        context.addHandler(sh);

        // --- Add the XML RPC servlet

        final XmlRpcServlet xmlRpcServlet = new XPMXMLRpcServlet(server,
                repository, taskManager);
        xmlRpcServlet.init(new XPMXMLRpcServlet.Config(xmlRpcServlet));

        final ServletHolder servletHolder = new ServletHolder(xmlRpcServlet);
        context.addServlet(servletHolder, "/xmlrpc");

        // --- Add the status servlet

        context.addServlet(new ServletHolder(new StatusServlet(taskManager)),
                "/status/*");

        // --- Add the status servlet

        context.addServlet(new ServletHolder(new TasksServlet(repository,
                taskManager)), "/tasks/*");

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

    void stop() throws Exception {
        server.stop();
    }

}
