package net.bpiwowar.xpm.connectors;

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

import net.bpiwowar.xpm.commands.UnixScriptProcessBuilder;
import net.bpiwowar.xpm.commands.XPMScriptProcessBuilder;
import net.bpiwowar.xpm.exceptions.LaunchException;
import net.bpiwowar.xpm.manager.scripting.Expose;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.manager.scripting.Help;
import net.bpiwowar.xpm.manager.scripting.ScriptContext;
import net.bpiwowar.xpm.scheduler.LauncherParameters;
import net.bpiwowar.xpm.scheduler.Scheduler;
import net.bpiwowar.xpm.utils.JsonAbstract;
import net.bpiwowar.xpm.utils.log.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

/**
 * net.bpiwowar.xpm.connectors
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Exposed
@JsonAbstract
public abstract class Launcher implements Serializable {
    /**
     * The notification URL
     */
    private URL notificationURL;

    /**
     * The environment to set
     */
    HashMap<String, String> environment = new HashMap<>();

    /**
     * The launcher
     */
    Connector connector;

    /**
     * The temporary directory
     */
    private Path temporaryDirectory;

    /**
     * Creates and returns a new process builder
     *
     * @return A process builder
     */
    public abstract AbstractProcessBuilder processBuilder() throws FileSystemException;

    /**
     * Creates a script builder
     *
     * @param scriptFile The path to the script file to createSSHAgentIdentityRepository
     * @param parameters
     * @return A builder
     * @throws FileSystemException if an exception occurs while accessing the script file
     */
    public XPMScriptProcessBuilder scriptProcessBuilder(Path scriptFile, LauncherParameters parameters) throws IOException {
        UnixScriptProcessBuilder xpmScriptProcessBuilder = new UnixScriptProcessBuilder(scriptFile, this);
        xpmScriptProcessBuilder.setNotificationURL(getNotificationURL());
        xpmScriptProcessBuilder.environment(environment);
        return xpmScriptProcessBuilder;
    }

    public Launcher(Connector connector) {
        this.connector = connector;
    }

    /**
     * Sets the notification URL
     */
    @Expose("set_notification_url")
    public void setNotificationURL(String url) throws MalformedURLException {
        setNotificationURL(new URL(url));
    }

    public void setNotificationURL(URL url) {
        this.notificationURL = url;
    }

    /**
     * Gets the notification URL
     *
     * @return
     */
    public URL getNotificationURL() {
        return notificationURL;
    }

    @Expose("env")
    @Help("Sets an environment variable and returns the old value (if any)")
    public String env(String key, String value) {
        return environment.put(key, value);
    }

    @Expose(value = "env")
    @Help("Gets the value of the environment variable")
    public String env(String key) {
        return environment.get(key);
    }


    public SingleHostConnector getMainConnector() {
        return connector.getMainConnector();
    }

    public Connector getConnector() {
        return connector;
    }

    @Expose("set_tmpdir")
    @Help("Sets the temporary directory for this launcher")
    public void setTemporaryDirectory(Path path) {
        this.temporaryDirectory = path;
    }

    public Path getTemporaryFile(String prefix, String suffix) throws IOException {
        if (temporaryDirectory != null) {
            return Files.createTempFile(temporaryDirectory, prefix, suffix);
        }

        Path path = this.connector.defaultTemporaryPath();
        return Files.createTempFile(path, prefix, suffix);
    }

    public String resolve(Path file) throws IOException {
        return getConnector().resolve(file);
    }


    transient HashMap<String, String> launcherEnvironment;

    @Expose()
    public String environment(String key) throws IOException, LaunchException, InterruptedException {
        if (launcherEnvironment == null) {
            AbstractProcessBuilder builder = processBuilder();
            builder.command("env");
            final Logger logger = ScriptContext.get().getMainLogger();
            final String result = builder.execute(logger);
            launcherEnvironment = new HashMap<>();
            for (String s : result.split("[\r\n]+")) {
                final int i = s.indexOf('=');
                final String _key = s.substring(0, i);
                final String value = s.substring(i + 1);
                launcherEnvironment.put(_key, value);
            }
        }
        return launcherEnvironment.get(key);
    }
}
