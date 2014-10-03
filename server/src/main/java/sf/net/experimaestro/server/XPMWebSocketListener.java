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

import org.apache.commons.lang.NotImplementedException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import sf.net.experimaestro.manager.Repositories;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.log.Logger;

import java.io.IOException;

/**
 * Web socket service
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 26/3/13
 */
public class XPMWebSocketListener extends WebSocketAdapter implements WebSocketListener {
    final static private Logger LOGGER = Logger.getLogger();
    private final JsonRPCMethods methods;


    public XPMWebSocketListener(Server server, Scheduler scheduler, Repositories repositories) {
        this.methods = new JsonRPCMethods(server, scheduler, repositories, new JSONRPCRequest() {
            @Override
            public void sendJSONString(String message) throws IOException {
                getRemote().sendString(message);
            }
        });

    }


    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        throw new NotImplementedException("Cannot handle binary frames");
    }

    @Override
    public void onWebSocketText(String message) {
        methods.handle(message);
    }



    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        super.onWebSocketClose(statusCode, reason);
        methods.close();
    }


}
