package net.bpiwowar.xpm.utils;

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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * Global information
 */
public class XPMInformation {
    private static XPMInformation xpmInformation;

    public String tags;
    public  String commitID;
    public  String message;
    public  boolean dirty;
    public  String commitTime;
    public  String branch;
    public  String remoteURL;

    private XPMInformation(Properties properties) {
        tags = properties.getProperty("git.tags");
        commitID = properties.getProperty("git.commit.id");
        message = properties.getProperty("git.commit.message.short");
        dirty = Boolean.parseBoolean(properties.getProperty("git.dirty"));
        commitTime = properties.getProperty("git.commit.time");
        branch = properties.getProperty("git.branch");
        remoteURL = properties.getProperty("git.remote.origin.url");
    }

    private XPMInformation() {
    }

    static public XPMInformation get() {

        if (xpmInformation == null) {
            final URL resource = XPMInformation.class.getResource("/VERSION");
            if (resource == null) {
                return new XPMInformation();
            }
            try (final InputStream inStream = resource.openStream()) {
                final Properties properties = new Properties();
                properties.load(inStream);
                xpmInformation = new XPMInformation(properties);
            } catch (IOException e) {
                xpmInformation = new XPMInformation();
            }
        }
        return xpmInformation;
    }
}
