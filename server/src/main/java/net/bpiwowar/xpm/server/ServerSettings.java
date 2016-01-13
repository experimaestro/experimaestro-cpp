package net.bpiwowar.xpm.server;

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

import bpiwowar.argparser.utils.Output;
import org.apache.commons.configuration.Configuration;
import net.bpiwowar.xpm.utils.log.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 29/3/13
 */
public class ServerSettings {
    final static private Logger LOGGER = Logger.getLogger();
    /**
     * Server name
     */
    String name = "?";
    /**
     * Default style: smoothness
     */
    Style style = Style.SMOOTHNESS;

    public ServerSettings(Configuration configuration) {
        try {
            name = configuration.getString("name", InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            LOGGER.error(e, "Could not get localhost name");
        }

        String styleName = configuration.getString("style", Style.SMOOTHNESS.toString()).toUpperCase();
        try {
            style = Style.valueOf(styleName);
        } catch (IllegalArgumentException e) {
            LOGGER.error(e, "Could not get parse style %s: it should be among [%s]", styleName,
                    Output.toString(",", Style.values()));
        }

    }


    enum Style {
        SMOOTHNESS, BLITZER
    }
}
