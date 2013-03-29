/*
 * This file is part of experimaestro.
 * Copyright (c) 2013 B. Piwowarski <benjamin@bpiwowar.net>
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

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import sf.net.experimaestro.manager.Repositories;
import sf.net.experimaestro.scheduler.Scheduler;

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
        factory.setCreator(new WebSocketCreator() {
            @Override
            public Object createWebSocket(UpgradeRequest req, UpgradeResponse resp) {
                return new XPMWebSocketListener(server, scheduler, repositories);
            }
        });
    }
}
