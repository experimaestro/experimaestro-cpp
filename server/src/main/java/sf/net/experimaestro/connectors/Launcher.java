package sf.net.experimaestro.connectors;

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

import sf.net.experimaestro.manager.scripting.Expose;
import sf.net.experimaestro.manager.scripting.Exposed;
import sf.net.experimaestro.manager.scripting.Help;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * sf.net.experimaestro.connectors
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Exposed
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
     * Creates and returns a new process builder
     *
     * @return A process builder
     */
    public abstract AbstractProcessBuilder processBuilder(SingleHostConnector connector) throws FileSystemException;

    /**
     * Returns a script process builder that can be run
     */
    public abstract XPMScriptProcessBuilder scriptProcessBuilder(SingleHostConnector connector, Path scriptFile) throws IOException;

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

    @Expose("env")
    @Help("Gets the value of the environment variable")
    public String env(String key) {
        return environment.get(key);
    }


}
