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


$.jsonRPC.setup({
    endPoint: '/json-rpc',
});


// custom css expression for a case-insensitive contains()
$.expr[':'].Contains = function (a, i, m) {
    return (a.textContent || a.innerText || "").toUpperCase().indexOf(m[3].toUpperCase()) >= 0;
};


function listFilter(list) { // header is any element, list is an unordered list
    // create and add the filter form to the header
    var form = $("<form>").attr({"class": "filterform", "action": "javascript:void(0)"}),
        input = $("<input>").attr({"class": "filterinput", "type": "text"});
    $(form).append(input).prependTo(list);

    $(input)
        .change(function () {
            var filter = $(this).val();
            if (filter) {
                // this finds all links in a list that contain the input,
                // and hide the ones not containing the input while showing the ones that do
                $(list).find("a:not(:Contains(" + filter + "))").parent().slideUp();
                $(list).find("a:Contains(" + filter + ")").parent().slideDown();
            } else {
                $(list).find("li").slideDown();
            }
            return false;
        })
        .keyup(function () {
            // fire the above change event after every letter
            $(this).change();
        });
}

/** Create an element */
$e = function (e) {
    return $(document.createElement(e));
}

/** Create a text node */
$t = function (s) {
    return $(document.createTextNode(s));
}

/** Transform json into HTML lists */
json2html = function (json) {
    if (json == null || typeof(json) != "object")
        return $t(json);

    var c = $e('ul');
    for (var key in json) {
        c.append($e('li').append($e('span').append($e("b").text(key + ": "))).append(json2html(json[key])));
    }
    return c;
}

function safe_tags(s) {
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

/** Called when an error occurs with JSON-RPC */
function jsonrpc_error(r) {
    noty({text: "Error" + r.error.code + ": " + r.error.message, type: 'error', timeout: 5000});
}


// --- actions on jobs: restart, remove
// TODO: invalidate a job (potentially recursively)
var resource_action_callback = function () {
    if (this.name == "restart") {
        $.jsonRPC.request('restart', {
            params: {"id": $(this).parent().attr("name"), "restart-done": true, "recursive": true},
            success: function (r) {
                noty({text: "Succesful restart", type: 'success', timeout: 5000});
            },
            error: jsonrpc_error
        });
    } else if (this.name == "delete") {
        var rsrcid = $(this).parent().attr("name");

        $("#delete-confirm").dialog({
            resizable: false,
            height: 140,
            modal: true,
            buttons: {
                "Yes, I understand": function () {
                    $(this).dialog("close");
                    $.jsonRPC.request('remove', {
                        params: {"id": rsrcid, "recursive": false},
                        success: function (r) {
                            noty({text: "Successful delete", type: 'success', timeout: 5000});
                        },
                        error: jsonrpc_error,
                    });
                },
                "Cancel": function () {
                    $(this).dialog("close");
                }
            }
        });
    }
}

// --- action: Get the details of a resource
var resource_link_callback = function () {
    var resourcePath = $(this).text();
    var resourceID = $(this).parent().attr("name");

    $.jsonRPC.request('getResourceInformation', {
        params: [resourceID],
        success: function (r) {
            $("#resource-detail-title").text("Resource #" + resourceID);
            $("#resource-detail-path").text(resourcePath);

            // Set the content
            $("#resource-detail-content").jstree(true).destroy();
            $("#resource-detail-content").empty().append(json2html(r.result));
            $("#resource-detail-content").jstree();

            // Activate the detail tab
            $("#tab-main").tabs("option", "active", 1);

            //history.pushState();

        },
        error: jsonrpc_error
    });
    return false;
};


$().ready(function () {
    // Links
    $(".xpm-resource-list img.link").on("click", resource_action_callback);
    $(".xpm-resource-list a").on("click", resource_link_callback);
    $("#header .links a").button();

    // Filter resource lists
    $(".xpm-resource-list").each(function () {
        listFilter($(this));
    });
    $("#resource-detail-content").jstree();

    // Tabs
    $(".tab").tabs();

    // Resource counts
    $("#resources").children("div").each(function () {
        var n = $(this).children("ul").children("li").size();
        var tabid = $(this).attr("id");
        $("#" + tabid + "-count").text(n);
    });

    // --- Now, listen to XPM events with a web socket
    websocket_protocol = window.location.protocol == "https" ? "wss" : "ws";
    websocket_url = websocket_protocol + "://" + window.location.host + "/web-socket";

    function create_websocket() {
        console.debug("WebSocket: Connecting to " + websocket_url);
        var websocket = new WebSocket(websocket_url);
        websocket.onmessage = function (e) {
            var decrement = function (e) {
                var old_counter_id = e.parents(".xpm-resource-list").attr("id") + "-count";
                var c = $("#" + old_counter_id);
                c.text(Number(c.text()) - 1);
            }

            //console.debug("Received: " + e.data);
            var r = $.parseJSON(e.data);
            if (r.error) {
                console.error("Error: " + e.data)
                return;
            }
            if (!r.result)
                return;
            r = r.result;
            if (r.event) switch (r.event) {
                case "STATE_CHANGED":
                    // Get the resource
                    var e = $("#R" + r.resource);

                    // Decrement old
                    decrement(e);

                    // Increment new state
                    var c = $("#state-" + r.state + "-count");
                    c.text(Number(c.text()) + 1);

                    // Put the item in the list
                    $("#state-" + r.state).children("ul").append(e);

                    break;

                case "RESOURCE_REMOVED":
                    // Get the resource
                    var e = $("#R" + r.resource);
                    if (e.length > 0) {
                        decrement(e);
                        e.remove();
                    }
                    break;

                case "PROGRESS":
                    var e = $("#R" + r.resource);
                    if (e.length < 1) {
                        console.warn("Resource " + r.resource + " does not exist (progress reported)!");
                    } else {
                        var pb = e.find("div.progressbar");
                        if (pb.length == 0) {
                            pb = $e("div");
                            pb.addClass("progressbar");
                            pb.progressbar({ value: r.progress * 100. });
                            pb.progressbar("option", "max", 100);
                            e.append(pb);
                        } else {
                            pb.progressbar( "option", "value", r.progress * 100 );
                        }
                    }
                    break;


                case "RESOURCE_ADDED":
                    var e = $("#R" + r.resource);

                    if (e.length > 0) {
                        console.warn("Resource " + r.resource + " already exists!");
                    } else {
                        var list = $("#state-" + r.state).children("ul");
                        var link = $e("a");
                        link.attr("href", "javascript:void(0)").append($t(r.locator));
                        link.on("click", resource_link_callback);

                        var restart_img = $("<img class='link' name='restart' alt='restart' src='/images/restart.png'/>");
                        restart_img.on("click", resource_action_callback);

                        var remove_img = $("<img class='link' name='restart' alt='restart' src='/images/delete.png'/>");
                        remove_img.on("click", resource_action_callback);

                        var item = $e("li")
                            .append(restart_img)
                            .append(remove_img)
                            .append(link);
                        item.attr("name", r.locator).attr("id", "R" + r.resource);

                        list.append(item);

                        var c = $("#state-" + r.state + "-count");
                        c.text(Number(c.text()) + 1);
                    }
                    break;

                default:
                    console.warn("Unhandled notification " + r.event);
                    break;
            }
        }

        websocket.onopen = function () {
            noty({text: "Web socket opened", type: 'information', timeout: 2000})
            $("#connection").attr("src", "/images/connect.png").attr("alt", "[connected]");
            var p = {id: 1, method: "listen", params: []};
            this.send(JSON.stringify(p));

            this.ping = setInterval(function () {
                if (websocket.readyState == WebSocket.OPEN) {
                    websocket.send(JSON.stringify({id: 2, method: "ping", params: []}));
                    console.debug("Sent ping");
                } else {
                    // If not open, remove ourselves
                    clearInterval(websocket.ping);
                }
            }, 120000);
        }

        websocket.onerror = function (e) {
            noty({text: "Web socket error: " + e, type: 'information', timeout: 5000})
        };
        websocket.onclose = function (e) {
            noty({text: "Web socket closed", type: 'information', timeout: 2000})
            $("#connection").attr("src", "/images/disconnect.png").attr("alt", "[disconnected]");
            clearInterval(this.ping);
        };
        return websocket;
    }

    var websocket = create_websocket();

    $("#connection").on("click", function () {
        switch (websocket.readyState) {
            case WebSocket.OPEN:
                websocket.close();
                break;
            case WebSocket.CLOSED:
                websocket = create_websocket();
                break;
            default:
                // Do nothing: transition between different
                websocket.onerror = function (e) {
                    noty({text: "Web Socket cannot be modified (closing or opening)", type: 'warning', timeout: 5000})
                };
        }
    });

});