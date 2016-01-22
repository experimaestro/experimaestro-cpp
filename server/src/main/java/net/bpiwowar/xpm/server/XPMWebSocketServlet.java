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

import net.bpiwowar.xpm.exceptions.WrappedException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import net.bpiwowar.xpm.manager.Repositories;
import net.bpiwowar.xpm.scheduler.Scheduler;

import java.io.IOException;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 26/3/13
 */
public class XPMWebSocketServlet extends WebSocketServlet {
    private final Server server;
    private final Scheduler scheduler;
    private final Repositories repositories;

    public XPMWebSocketServlet(Server server, Scheduler scheduler, Repositories repositories) {
        this.server = server;
        this.scheduler = scheduler;
        this.repositories = repositories;
    }

    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.setCreator((req, resp) -> {
            try {
                return new XPMWebSocketListener(server, scheduler, repositories);
            } catch (IOException e) {
                throw new WrappedException(e);
            }
        });
    }
}
