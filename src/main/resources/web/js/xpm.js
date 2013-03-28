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

/*
// WebSocket
var socket = new WebSocket("ws://localhost:12346/web-socket");
var p = { command: "run-javascript", args: { files: [ "pre", "logger.info(\"hello world\"" ]} };
socket.onmessage = function(e){ alert(e); }
socket.send(JSON.stringify(p));
socket.close();
*/

//alert(window.location.hostname);

/*
// XML-RPC call

var r = $.xmlrpc({
    url: 'http://localhost:12346/xmlrpc',
    methodName: 'Server.listJobs',
    params: ['', ['DONE']],
    success: function(response, status, jqXHR) {
        alert("OK: " + JSON.stringify(response));
    },
    error: function(jqXHR, status, error) { alert(error); }
});
*/