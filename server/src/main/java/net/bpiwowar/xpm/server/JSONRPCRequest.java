package sf.net.experimaestro.server;

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

import org.json.simple.JSONObject;

import java.io.IOException;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 28/3/13
 */
public abstract class JSONRPCRequest {
    abstract protected void sendJSONString(String message) throws IOException;

    public void endMessage(String requestID, Object message) throws IOException {
        JSONObject answer = getJSONPartialAnswer(requestID);
//        answer.put("error", null);
        answer.put("result", message);

        sendJSONString(answer.toJSONString());

    }

    private JSONObject getJSONPartialAnswer(String requestID) {
        JSONObject answer = new JSONObject();
        answer.put("jsonrpc", "2.0");
        answer.put("id", requestID);
        return answer;
    }

    void error(String requestID, int code, String message) throws IOException {
        JSONObject answer = getJSONPartialAnswer(requestID);
        answer.put("result", null);

        JSONObject errorMessage = new JSONObject();
        errorMessage.put("code", code);
        errorMessage.put("message", message);
        answer.put("error", errorMessage);

        sendJSONString(answer.toJSONString());
    }

    /**
     * Send a message to the client
     */
    public void message(Object message) throws IOException {
        endMessage(null, message);
    }
}
