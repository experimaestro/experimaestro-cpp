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

import com.google.gson.Gson;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public abstract class JSONRPCRequest {
    abstract protected void sendJSONString(String message) throws IOException;

    public void endMessage(String requestID, Object result) throws IOException {
        JsonObject answer = getJSONPartialAnswer(requestID);

        if (result instanceof Iterable) {
            List<Object> list = new ArrayList<>();
            ((Iterable) result).forEach(list::add);
            result = list;
        } else if (result instanceof Stream) {
            List<Object> list = new ArrayList<>();
            ((Stream) result).forEach(list::add);
            result = list;
        }

        Gson gson = new Gson();

        if (result != null) answer.add("result", gson.toJsonTree(result, result.getClass()));
        else answer.add("result", JsonNull.INSTANCE);

        sendJSONString(gson.toJson(answer));

    }

    private JsonObject getJSONPartialAnswer(String requestID) {
        JsonObject answer = new JsonObject();
        answer.addProperty("jsonrpc", "2.0");
        answer.addProperty("id", requestID);
        return answer;
    }

    void error(String requestID, int code, String message) throws IOException {
        JsonObject answer = getJSONPartialAnswer(requestID);
//        answer.put("result", null);

        JsonObject errorMessage = new JsonObject();
        errorMessage.add("code", new JsonPrimitive(code));
        errorMessage.add("message", new JsonPrimitive(message));
        answer.add("error", errorMessage);

        Gson gson = new Gson();

        sendJSONString(gson.toJson(answer));
    }

    /**
     * Send a message to the client
     */
    public void message(Object message) throws IOException {
        endMessage(null, message);
    }
}
