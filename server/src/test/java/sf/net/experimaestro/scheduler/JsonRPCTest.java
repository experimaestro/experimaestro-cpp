package sf.net.experimaestro.scheduler;

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

import com.google.common.collect.ImmutableMap;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.client.ConnectionConfigurator;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2Session;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionException;
import junit.framework.Assert;
import org.apache.ws.commons.util.Base64;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import sf.net.experimaestro.connectors.LocalhostConnector;
import sf.net.experimaestro.tasks.ServerTask;
import sf.net.experimaestro.utils.XPMEnvironment;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;

import static java.lang.String.format;

/**
 * Tests the different Json-RPC calls
 */
public class JsonRPCTest extends XPMEnvironment {
    private static ServerTask server;
    private static JSONRPC2Session rpcSession;

    @BeforeSuite
    public static void setup() throws Throwable {
        server = prepare();
        final URL jsonRPCUrl = new URL("http", "localhost", server.getPort(), ServerTask.JSON_RPC_PATH);
        rpcSession = new JSONRPC2Session(jsonRPCUrl);
        rpcSession.setConnectionConfigurator(new BasicAuthenticator());
    }

    @Test
    void removeResource() throws IOException, JSONRPC2SessionException, SQLException {
        File jobDirectory = mkTestDir();

        XPMEnvironment.getDirectory();
        final Resource resource = new Resource(Scheduler.get().getLocalhostConnector(), jobDirectory.toPath().resolve("resource-1"));
        resource.save();
        final long id = resource.getId();


        // Now, RPC call to delete
        JSONRPC2Request request = new JSONRPC2Request("remove", 0);
        request.setNamedParams(ImmutableMap.of("id", Long.toString(id)));
        rpcSession.send(request);

        // Check that the resource was removed
        final Resource _resource = Resource.getById(id);
        Assert.assertNull("Resource has not been deleted", _resource);
    }

    public static class BasicAuthenticator implements ConnectionConfigurator {
        public void configure(HttpURLConnection connection) {
            // add custom HTTP header
            final String authString = Base64.encode(format("%s:%s", testUser, testPassword).getBytes()).trim();
            connection.addRequestProperty("Authorization", "Basic " + authString);
        }
    }
}
