package net.bpiwowar.xpm.server.rpc;

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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.bpiwowar.xpm.manager.Repositories;
import net.bpiwowar.xpm.scheduler.Scheduler;
import net.bpiwowar.xpm.server.ServerSettings;
import net.bpiwowar.xpm.utils.log.Logger;
import org.eclipse.jetty.server.Server;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Json-RPC2 servlet
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class JsonRPCServlet extends HttpServlet {
    final static private Logger LOGGER = Logger.getLogger();
    private final JsonRPCSettings settings;

    public JsonRPCServlet(Server server, ServerSettings serverSettings, Scheduler scheduler, Repositories repository) {
        this.settings = new JsonRPCSettings(scheduler, repository, server, serverSettings);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            JsonCallHandler handler = new JsonCallHandler(req, resp);
            final String queryString = req.getQueryString();
            if (queryString == null) {
                ServletOutputStream outputStream = resp.getOutputStream();
                OutputStreamWriter writer = new OutputStreamWriter(outputStream);
                writer.write("Error");
                outputStream.close();
                return;
            }
            JsonParser parser = new JsonParser();
            handler.handleJSON(parser.parse(queryString).getAsJsonObject());
        } catch (RuntimeException e) {
            LOGGER.error(e, "Error while handling request");
        } finally {
            ServletOutputStream outputStream = resp.getOutputStream();
            outputStream.flush();
            outputStream.close();
        }
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        try {
            ServletInputStream inputStream = req.getInputStream();
            InputStreamReader reader = new InputStreamReader(inputStream);
            JsonCallHandler handler = new JsonCallHandler(req, resp);

            JsonParser parser = new JsonParser();
            final JsonObject message = parser.parse(reader).getAsJsonObject();
            handler.handleJSON(message);
        } finally {
            ServletOutputStream outputStream = resp.getOutputStream();
            outputStream.flush();
            outputStream.close();

        }
    }

    private class JsonCallHandler {
        private final JsonRPCMethods jsonRPCMethods;

        private JsonCallHandler(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
            ServletOutputStream outputStream = resp.getOutputStream();
            final PrintWriter pw = new PrintWriter(outputStream);

            resp.setContentType("application/json");
            resp.setStatus(HttpServletResponse.SC_OK);

            jsonRPCMethods = new JsonRPCMethods(settings, new JSONRPCRequest() {
                @Override
                public void sendJSONString(String message) throws IOException {
                    pw.print(message);
                    pw.flush();
                }
            });
        }

        public void handleJSON(JsonObject message) {
            jsonRPCMethods.handleJSON(message);
        }
    }

}
