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

import com.google.gson.Gson;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.bpiwowar.xpm.server.rpc.JSONRPCRequest;
import net.bpiwowar.xpm.server.rpc.JsonRPCMethods;
import net.bpiwowar.xpm.server.rpc.JsonRPCSettings;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.api.WebSocketListener;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Web socket service
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class XPMWebSocketListener extends WebSocketAdapter implements WebSocketListener {
    final static private Logger LOGGER = LogManager.getFormatterLogger();

    private final JsonRPCMethods methods;
    private final JSONRPCRequest mos;

    static ThreadLocal<XPMWebSocketListener> threadLocal = new ThreadLocal<>();
    private final BufferedWriter errorStream;

    public XPMWebSocketListener(JsonRPCSettings settings) throws IOException, NoSuchMethodException {
        mos = new JSONRPCRequest() {
            @Override
            public void sendJSONString(String message) throws IOException {
                sendString(message);
            }
        };
        errorStream = mos.getRequestErrorStream();
        this.methods = new JsonRPCMethods(settings, true, mos);

    }

    private void sendString(String message) throws IOException {
        try {
            final RemoteEndpoint remote = getRemote();
            if (remote != null) {
                remote.sendString(message);
            } else {
                LOGGER.info("Could not send message: %s", message);
            }
        } catch (IllegalStateException e) {
            // Ignore: remote can be blocking
        }
    }


    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        throw new NotImplementedException("Cannot handle binary frames");
    }

    @Override
    public void onWebSocketText(String message) {
        try (final CloseableThreadContext.Instance ctc = CloseableThreadContext.put("xpm-websocket", "1")) {
            threadLocal.set(this);
            methods.handle(message);
        } finally {
            try {
                errorStream.flush();
            } catch (IOException ignore) {
                // Ignore this
            }
            threadLocal.set(null);
        }
    }


    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        super.onWebSocketClose(statusCode, reason);
        methods.close();
        ThreadContext.clearMap();
    }


    public static void write(LogEvent event, byte[] bytes) throws IOException {
        final XPMWebSocketListener self = threadLocal.get();
        if (self != null) {
            Gson gson = new Gson();

            JsonObject answer = new JsonObject();
            answer.addProperty("jsonrpc", "2.0");
            answer.add("id", JsonNull.INSTANCE);

            JsonObject result = new JsonObject();
            result.addProperty("type", "log");
            result.addProperty("level", event.getLevel().toString());
            result.addProperty("message", new String(bytes));
            answer.add("result", result);

            self.sendString(gson.toJson(answer));
        }

    }
}
