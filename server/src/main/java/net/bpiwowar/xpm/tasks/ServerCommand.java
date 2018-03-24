/**
 *
 */
package net.bpiwowar.xpm.tasks;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.bpiwowar.xpm.scheduler.Scheduler;
import net.bpiwowar.xpm.server.ContentServlet;
import net.bpiwowar.xpm.server.NotificationServlet;
import net.bpiwowar.xpm.server.XPMWebSocketServlet;
import net.bpiwowar.xpm.server.rpc.JsonRPCMethods;
import net.bpiwowar.xpm.server.rpc.JsonRPCServlet;
import net.bpiwowar.xpm.utils.PathTypeAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.security.AbstractLoginService;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;
import picocli.CommandLine;

import java.io.File;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.lang.String.format;

/**
 * The server displays information about the tasks and responds to XML RPC tasks
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@CommandLine.Command(name = "server")
public class ServerCommand implements Runnable {
    public static final String KEY_SERVER_SOCKET = "server.socket";
    public static final String XPM_REALM = "xpm-realm";
    final static Logger LOGGER = LogManager.getFormatterLogger();
    public static final String JSON_RPC_PATH = "/json-rpc";

    /**
     * The scheduler
     */
    private Scheduler scheduler;

    /**
     * Should we wait ?
     */
    boolean wait = true;

    /**
     * The port the web server was started
     */
    private int port;

    private Server webServer;
    private ServerSettings configuration;

    public void wait(boolean wait) {
        this.wait = wait;
    }

    public void setConfiguration(ServerSettings configuration) {
        this.configuration = configuration;
    }


    static public class PasswordSettings {
        public String user;
        public String password;
        public String[] roles;
    }

    static public class ServerSettings {
        public int port = 8080;
        public Path database;
        public ArrayList<PasswordSettings> passwords;
        public String name;
    }


    static public class Configuration {
        ServerSettings server;
    }

    /**
     * Server thread
     */
    public void run() {
        try {
            if (configuration == null) {
                final String envConfFile = System.getenv("EXPERIMAESTRO_CONFIG_FILE");
                Path file = null;
                if (envConfFile != null) {
                    file = Paths.get(envConfFile);
                    LOGGER.info("Using configuration file set in environment: '" + file + "'");
                } else {
                    file = Paths.get(System.getProperty("user.home"), ".experimaestro", "settings.json");
                    LOGGER.info("Using the default configuration file " + file);
                }

                LOGGER.info("Reading configuration from {}", file);
                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.registerTypeAdapter(Path.class, new PathTypeAdapter());
                Gson gson = gsonBuilder.create();
                Configuration cfg = gson.fromJson(Files.newBufferedReader(file), Configuration.class);
                configuration = cfg.server;
            }


            // --- Get the port
            port = configuration.port;
            LOGGER.info("Starting server on port %d", port);

            // --- Set up the task manager
            if (configuration.database == null)
                throw new IllegalArgumentException("No 'database' in 'server' section of the configuration file");

            File taskmanagerDirectory = configuration.database.toFile();
            scheduler = new Scheduler(taskmanagerDirectory);

            final String baseURL = format("http://%s:%d", InetAddress.getLocalHost().getHostName(), port);
            LOGGER.info("Server URL is %s", baseURL);
            scheduler.setURL(baseURL);

            webServer = new Server();

            // TCP-IP socket
            ServerConnector connector = new ServerConnector(webServer);
            connector.setPort(port);
            webServer.addConnector(connector);

            // Unix domain socket
//        if (configuration.containsKey(KEY_SERVER_SOCKET)) {
//            // TODO: move this to the target class
//            String libraryPath = System.getProperty("org.newsclub.net.unix.library.path");
//            if (libraryPath == null) {
//                URL url = ServerCommand.class.getProtectionDomain().getCodeSource().getLocation();
//                File file = new File(url.toURI());
//                while (file != null && !new File(file, "native-libs").exists()) {
//                    file = file.getParentFile();
//                }
//                if (file == null)
//                    throw new UnsatisfiedLinkError("Cannot find the native-libs directory");
//                file = new File(file, "native-libs");
//
//                LOGGER.info("Using path for junixsocket library [%s]", file);
//                System.setProperty("org.newsclub.net.unix.library.path", file.getAbsolutePath());
//            }
//
//            String socketSpec = configuration.getString(KEY_SERVER_SOCKET);
//            UnixSocketConnector unixSocketConnector = new UnixSocketConnector(webServer);
//            unixSocketConnector.setSocketFile(new File(socketSpec));
//            webServer.addConnector(unixSocketConnector);
//        }


            HandlerList collection = new HandlerList();

            // --- Non secure context

            ServletContextHandler nonSecureContext = new ServletContextHandler(collection, "/");
            nonSecureContext.getServletHandler().setEnsureDefaultServlet(false); // no 404 default page
            nonSecureContext.addServlet(new ServletHolder(new NotificationServlet(configuration, scheduler)), "/notification/*");

            // --- Sets the password on all pages

            ServletContextHandler context = new ServletContextHandler(collection, "/");
            ConstraintSecurityHandler csh = getSecurityHandler(configuration);
            context.setSecurityHandler(csh);

            // --- Add the JSON RPC servlet

            final JsonRPCServlet jsonRpcServlet = new JsonRPCServlet(webServer, configuration, scheduler);
            JsonRPCMethods.initMethods();
            final ServletHolder jsonServletHolder = new ServletHolder(jsonRpcServlet);
            context.addServlet(jsonServletHolder, JSON_RPC_PATH);

            // --- Add the web socket servlet

            final XPMWebSocketServlet webSocketServlet = new XPMWebSocketServlet(webServer, scheduler, configuration);
            final ServletHolder webSocketServletHolder = new ServletHolder(webSocketServlet);
            context.addServlet(webSocketServletHolder, "/web-socket");


            // --- Add the default servlet

            context.addServlet(new ServletHolder(new ContentServlet(configuration)), "/*");

            // final URL warUrl =
            // this.getClass().getClassLoader().getResource("web");
            // final String warUrlString = warUrl.toExternalForm();
            // server.setHandler(new WebAppContext(warUrlString, "/"));

            // --- Sets the main handler
            webServer.setHandler(collection);

            // --- start the server and wait


            webServer.start();

            if (wait) {
                webServer.join();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        LOGGER.info("Servers are stopped. Clean exit!");

    }

    private ConstraintSecurityHandler getSecurityHandler(ServerSettings settings) {
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

        LoginService loginService = new LoginService();
        loginService.setName(XPM_REALM);

        // Read passwords
        for (PasswordSettings password : settings.passwords) {
            LOGGER.info("Adding user %s", password.user);
            loginService.putUser(password.user, new Password(password.password), password.roles);
        }

        ConstraintSecurityHandler csh = new ConstraintSecurityHandler();
        csh.setRealmName(XPM_REALM);
        csh.setAuthenticator(new BasicAuthenticator());
        csh.setConstraintMappings(new ConstraintMapping[]{cm});
        csh.addConstraintMapping(cm);
        csh.setLoginService(loginService);
        return csh;
    }


    public Scheduler getScheduler() {
        return scheduler;
    }

    public int getPort() {
        return port;
    }

    public void close() throws Exception {
        scheduler.close();

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                boolean stopped = false;
                try {
                    webServer.stop();
                    stopped = true;
                } catch (Exception e) {
                    LOGGER.error("Could not stop properly jetty", e);
                }
                if (!stopped)
                    synchronized (this) {
                        try {
                            wait(10000);
                        } catch (InterruptedException e) {
                            LOGGER.error(e);
                        }
                        System.exit(1);

                    }
            }
        }, 2000);

    }

    private static class LoginService extends AbstractLoginService {
        HashMap<String, UserPrincipal> users = new HashMap<>();
        HashMap<String, String[]> roles = new HashMap<>();

        @Override
        protected String[] loadRoleInfo(UserPrincipal user) {
            return roles.get(user.getName());
        }

        @Override
        protected UserPrincipal loadUserInfo(String username) {
            return users.get(username);
        }

        public void putUser(String user, Password password, String[] roles) {
            this.roles.put(user, roles);
            this.users.put(user, new UserPrincipal(user, password));
        }
    }

}
