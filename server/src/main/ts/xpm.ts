/// <reference path="typings/index" />

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

interface JQuery {
    tagit(options:any):void;
}

interface BlahBlah extends HTMLElement {
    state:string;
}


/** Called when an error occurs with JSON-RPC */
function jsonrpc_error(r) {
    noty({text: "Error" + r.code + ": " + r.message, type: 'error', timeout: 5000});
}

/** Create an element */
var $e = function (e) {
    return $(document.createElement(e));
};

/** Create a text node */
var $t = function (s) {
    return $(document.createTextNode(s));
};

function change_counter(state, delta) {
    var c = $("#state-" + state + "-count");
    c.text(Number(c.text()) + delta);
}

/** Transform json into HTML lists */
var json2html = function (json:any) {
    if (json == null || typeof(json) != "object")
        return $t(json);

    var c = $e('ul');
    for (var key in json) {
        c.append($e('li').append($e('span').append($e("b").text(key + ": "))).append(json2html(json[key])));
    }
    return c;
}

class Resource {
    node: JQuery;
    state: string;
    id: string;

    constructor(li:HTMLElement) {
        this.node = $(li);
        this.state = li[0].state;
        this.id = li.attr("name")
    }

    static fromElementID(id: string) {
        var e = document.getElementById(id);
        if (e != null) {
            return new Resource(e);
        }
    }
}

/** Get the resource to which a DOM element "belongs"
 * @param e The DOM element as a jQuery object
 * @return An object with id, state
 */
function get_resource(e) {
    var li = e.parentsUntil("li.resource").last().parent();
    return new Resource(li);
}

function resource_progress(r) {
    if (!r.progress) return;

    var e = $("#R" + r.id);
    if (e.length < 1) {
        console.warn("Resource " + r.id + " does not exist (progress reported)!");
    } else {
        var pb = e.find("div.progressbar");
        if (pb.length == 0) {
            pb = $e("div").append($e("div").append($t("Running")).addClass("progress-label"));
            pb.addClass("progressbar");
            pb.progressbar({value: r.progress * 100.});
            pb.progressbar("option", "max", 100);
            e.prepend(pb);
            e.addClass("with-progressbar");
        } else {
            pb.progressbar("option", "value", r.progress * 100);
        }
    }
};

class XPM {
    server:any;
    task2resource = {};
    taskname2id = {};
    filtered_tasks = new Set();
    ping:any;
    xpm_info_loaded:boolean;

    experiment_timestamp:number;

    request(name, params) {
        this.server.call(name, params.params, params.success, params.error)
    }

    handle_ws_open(event) {
        $("#connection").attr("src", "/images/connect.png").attr("alt", "[connected]");

        // Set some pinging...
        this.ping = setInterval(function () {
            this.server.notify("ping");
            console.debug("Sent ping");
        }, 120000);

        this.server.call("listen", {}, () => {
        }, jsonrpc_error);
        this.get_experiments();
    };

    /** Get the experiments */
    get_experiments() {
        this.server.call('experiments.latest-names', {},
            function (r) {
                var select = $("#experiment-chooser");
                select.children().remove();
                $.each(r, function (ix, e) {
                    select.append($e("option").append($t(e.identifier)));
                });
                this.load_experiment(0);
            },

            jsonrpc_error
        )
    }

    load_experiment(timestamp:number) {
        var select = $("#experiment-chooser");
        var tasks_chooser = $("#task-chooser");
        var experiment = select.find("option:selected").text();

        // Remove resources and counts
        $("#resources").children().remove();
        $("#state-chooser").find(".state-count").text(0);


        this.server.call('experiments.resources', {identifier: experiment, timestamp: timestamp},
            function (r) {
                var tasks = r.tasks;
                var available_tasks = [];
                var set = new Set();
                for (var _tid in tasks) {
                    let tid = Number.parseInt(_tid);
                    this.task2resource[tid] = []

                    var tname = tasks[tid];
                    if (!set.has(tname)) {
                        this.taskname2id[tname] = [tid];
                        set.add(tname);
                        available_tasks.push(tname);
                    } else {
                        this.taskname2id[tname].push(tid);
                    }
                }

                function change_state(taskname, display) {
                    var taskids = this.taskname2id[taskname];
                    for (var i = 0; i < taskids.length; ++i) {
                        var resources = this.task2resource[taskids[i]];
                        if (display) this.filtered_tasks.add(taskids[i]);
                        else this.filtered_tasks.delete(taskids[i]);
                        for (var j = 0; j < resources.length; ++j) {
                            if (display) {
                                $(resources[j]).removeClass("notintask");
                            } else {
                                $(resources[j]).addClass("notintask");
                            }
                        }
                    }
                }

                var tag_count = 0;
                tasks_chooser.tagit({
                    placeholderText: "Filter by task",
                    autocomplete: {
                        source: function (request, response) {
                            // delegate back to autocomplete, but extract the last term
                            response($.ui.autocomplete.filter(available_tasks, request.term));
                        }
                    },
                    beforeTagAdded: function (event, ui) {
                        return ui.tagLabel in this.taskname2id;
                    },

                    afterTagAdded: function (event, ui) {
                        // Remove everything for the first one
                        if (tag_count == 0) $("#resources").children().addClass("notintask");
                        ++tag_count;
                        change_state(ui.tagLabel, true);
                    },

                    afterTagRemoved: function (event, ui) {
                        --tag_count;
                        if (tag_count == 0) {
                            this.filtered_tasks.clear();
                            $("#resources").children().removeClass("notintask");
                        } else {
                            change_state(ui.tagLabel, "none");
                        }
                    }

                });

                var date = new Date(r.experiment.timestamp);
                var ts = $("#experiment-timestamp");
                ts.text(date.toString());
                console.log("New timestamp: " + r.experiment.timestamp);
                this.experiment_timestamp = r.experiment.timestamp;
                $.each(r.resources, function (ix, v) {
                    var r = this.add_resource(v);
                    this.task2resource[v.taskid].push(r);
                });
            },
            jsonrpc_error
        );
    }

    makelinks(e) {
        e.find(".link").on("click", this.resource_action_callback);
    }

    handle_message(resp) {
        //console.debug("Received: " + e.data);
        var r = $.parseJSON(resp.data);
        if (r.error) {
            console.error("Error: " + resp.data);
            return;
        }
        if (!r.result)
            return;

        // Process result
        r = r.result;
        if (r.event) switch (r.event) {
            case "STATE_CHANGED":
            {
                // Get the resource
                var e = Resource.fromElementID("#R" + r.id);
                if (e != null) {
                    r.state = r.state.toLowerCase();

                    // Remove progress bars
                    e.node.removeClass("with-progressbar");
                    e.node.find("div.progressbar").remove();

                    // Update counters
                    var oldstate = e.get(0).state;
                    change_counter(oldstate, -1);
                    change_counter(r.state, +1);

                    // Put the item in the list
                    e.node.removeClass("state-" + oldstate)
                        .addClass("state-" + r.state);
                    e.get(0).state = r.state;
                }

                break;
            }

            case "RESOURCE_REMOVED":
            {
                // Get the resource
                var e = $("#R" + r.id);
                if (e.length > 0) {
                    decrement(e);
                    e.remove();
                }
                break;
            }

            case "PROGRESS":
                resource_progress(r);
                break;


            case "RESOURCE_ADDED":
                add_resource(r);
                break;

            case "EXPERIMENT_RESOURCE_ADDED":
                var current = current_experiment();
                if (current.name == r.name && current.time == r.timestamp) {
                    add_resource(r.resource);
                }
                break;

            case "EXPERIMENT_ADDED":
                // If this is the current experiment, remove everything
                var current = current_experiment();
                if (r.name == current.name) {
                    load_experiment(r.timestamp);
                }
                break;

            default:
                console.warn("Unhandled notification " + r.event);
                break;
        }
    }


    add_resource(r) {
        r.state = r.state.toLowerCase();
        console.log("Adding resource with id " + r.id + " and state " + r.state);
        var e = $("#R" + r.id);

        if (e.length > 0) {
            console.warn("Resource " + r.id + " already exists!");
        } else {
            var link = $e("a")
                .attr("href", "javascript:void(0)")
                .append($("<span class='locator'>" + r.locator + "</span>"))
                .on("click", this.resource_link_callback);

            var item = $e("li")
                .addClass("state-" + r.state)
                .addClass("resource")
                .attr("name", r.id)
                .attr("id", "R" + r.id)
                .append($e("span").addClass("resource-actions")
                    .append($("<span class='resource-id'>" + r.id + "</span>"))
                    .append($("<i class=\"fa fa-eye link\" title='View' name='fileview-out'></i>"))
                    .append($("<i class=\"fa fa-eye link\" style=\"color: red\" title='View' name='fileview-err'></i>"))
                    .append($("<i class=\"fa fa-folder-o link\" title='Copy folder path' name='copyfolderpath'></i>"))
                    .append($("<i class=\"fa fa-retweet link\" title='Restart job' name='restart'></i>"))
                    .append($("<i class=\"fa fa-stop link\" title='Kill job' name='kill'></i>"))
                    .append($("<i class=\"fa fa-trash-o link\" title='Delete resource' name='delete'></i>"))
                ).append(
                    $e("div")
                        .addClass("resource-link")
                        .append(link)
                );

            (<BlahBlah>item.get(0)).state = r.state;

            if (this.filtered_tasks.size > 0 && !this.filtered_tasks.has(r.taskid)) {
                item.addClass("notintask");
            }

            $("#resources").append(item);
            this.makelinks(item);

            change_counter(r.state, +1);
            if (r.state == "running" && r.progress > 0) {
                this.resource_progress(r);
            }

            return item;
        }
    };

    // --- actions on jobs: restart, remove
    resource_action_callback() {
        var name = this.name ? this.name : this.getAttribute("name");
        if (!name) {
            alert("internal error: no name given for action in [" + this.tagName + "/" + typeof(this) + "]");
            return;
        }

        var r = get_resource($(this));

        if (name == "restart") {
            var request = function (restartDone) {
                this.request('invalidate', {
                    params: {"ids": [r.id], "keep-done": !restartDone, "recursive": true, "restart": true},
                    success: function (resp) {
                        noty({
                            text: "Succesful restart (" + resp.result + " jobs restarted)",
                            type: 'success',
                            timeout: 5000
                        });
                    },
                    error: jsonrpc_error
                });
            };


            if (r.state == "done") {
                $("#restart-confirm").dialog({
                    resizable: false,
                    height: 140,
                    modal: true,
                    open: function () {
                        $(this).siblings('.ui-dialog-buttonpane').find('button:eq(1)').focus();
                    },
                    buttons: {
                        "Yes, I understand": function () {
                            $(this).dialog("close");
                            request(true);
                        },
                        "Cancel": function () {
                            $(this).dialog("close");
                        }
                    }
                });
            } else {
                request(false);
            }
        }

        else if (name == "delete") {
            $("#delete-confirm").dialog({
                resizable: false,
                height: 140,
                modal: true,
                open: function () {
                    $(this).siblings('.ui-dialog-buttonpane').find('button:eq(1)').focus();
                },
                buttons: {
                    "Yes, I understand": function () {
                        $(this).dialog("close");
                        this.request('remove', {
                            params: {"id": r.id, "recursive": false},
                            success: function () {
                                // We just notify - but wait for the server notification to
                                // remove the job from the interface
                                noty({text: "Successful delete", type: 'success', timeout: 5000});
                            },
                            error: jsonrpc_error
                        });
                    },
                    "Cancel": function () {
                        $(this).dialog("close");
                    }
                }
            });
        }

        else if (name == "kill") {
            $("#kill-confirm").dialog({
                resizable: false,
                height: 140,
                modal: true,
                open: function () {
                    $(this).siblings('.ui-dialog-buttonpane').find('button:eq(1)').focus();
                },
                buttons: {
                    "Yes, I understand": function () {
                        $(this).dialog("close");
                        this.request('kill', {
                            params: {"jobs": [r.id]},
                            success: function () {
                                // We just notify - but wait for the server notification to
                                // remove the job from the interface
                                noty({text: "Successfully killed job " + r.id, type: 'success', timeout: 5000});
                            },
                            error: jsonrpc_error
                        });
                    },
                    "Cancel": function () {
                        $(this).dialog("close");
                    }
                }
            });
        }

        else if (name == "copyfolderpath") {
            this.request('paths', {
                params: {id: r.id},
                success: function (resp) {
                    var keys = Object.keys(resp);
                    var dl = $e("dl");
                    for (var key in resp) {
                        dl.append($e("dt").append(key)).append($e("dd").append(resp[key]))
                    }
                    $("#clipboard-content").replaceWith(dl);
                    dl.attr("id", "clipboard-content");
                    $("#clipboard").dialog({
                        maxWidth: 600,
                        width: "70%",
                        title: "Select the path"
                    });
                }
            });
        } else if (name.startsWith("fileview-")) {
            var isStdErr = name.endsWith("err");

            var fileview = function (uri) {
                this.request('view-file', {
                    params: {
                        uri: uri,
                        position: -4096,
                        size: 4096
                    },
                    success: function (resp) {
                        var a = $e("pre");
                        a.attr("id", "file-viewer-" + r.id);
                        a.text(resp);
                        $("body").append(a);
                        a.dialog({
                            title: (isStdErr ? "stderr" : "stdout") + " [" + r.id + "] " + uri,
                            width: "80%"
                        });
                    }
                });
            };

            this.request('resource-path', {
                params: {
                    id: r.id,
                    type: isStdErr ? 'stderr' : 'stdout'
                },
                success: function (resp) {
                    fileview(resp)
                }
            });
        }
    };

    resource_link_callback() {
        var resourcePath = $(this).text();
        var resourceID = $(this).parent().parent().attr("name");

        this.server.call('getResourceInformation', {id: resourceID},
            function (r) {
                $("#resource-detail-title").text("Resource #" + resourceID);
                $("#resource-detail-path").text(resourcePath);

                // Set the content
                var rdc = $("#resource-detail-content");
                rdc.jstree(true).destroy();
                rdc.empty().append(json2html(r));
                rdc.jstree();

                $(function () {
                    $("#resource-detail").dialog({
                        "maxWidth": 600,
                        "width": "70%",
                    });
                });

            },
            jsonrpc_error
        );
        return false;
    };


    show_xpm_info() {
        var e = $("#xpm-info");
        if (this.xpm_info_loaded) return true;

        this.server.call('buildInformation', {},
            function (r) {
                this.xpm_info_loaded = true;
                e.append($e("h2")
                    .append($t("Build information")))
                    .append($e("dl").append(
                        $e("dt").append($t("Branch")),
                        $e("dd").append(r.result.branch),
                        $e("dt").append($t("Commit hash")),
                        $e("dd").append(r.result.commitID),
                        $e("dt").append($t("Dirty")),
                        $e("dd").append(r.result.dirty ? "True" : "False"),
                        $e("dt").append($t("Commit ID")),
                        $e("dd").append(r.result.commitID),
                        $e("dt").append($t("Tags")),
                        $e("dd").append(r.result.tags)
                    ));
            },
            jsonrpc_error
        );
    }
}

$().ready(function () {

    let xpm = new XPM();

    /**
     * custom css expression for a case-insensitive contains()
     * @return {boolean}
     */
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

    function safe_tags(s) {
        return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }


    function show_class_documentation() {
        var classname = $(this).find("option:selected").text();
        xpm.server.call("documentation.methods", {classname: classname},
            function (r) {
                alert(JSON.stringify(r));
            },
            jsonrpc_error
        );
    }


    function current_experiment() {
        return {
            name: $("#experiment-chooser").find("option:selected").text(),
            time: xpm.experiment_timestamp
        };
    }


    // When changing, load experiment
    $("#experiment-chooser").change(function () {
        xpm.load_experiment(0)
    });

    function showexperiments(element) {
        var width = 960,
            height = 500;


        var svg = d3.select(element.find("svg").get(0))
            .attr("width", width)
            .attr("height", height);

        var force = d3.layout.force()
            .gravity(.05)
            .distance(100)
            .charge(-100)
            .size([width, height]);

        var rpcData = {
            "method": "experiments",
            "params": {},
            "jsonrpc": "2.0",
            "id": 1
        };

        d3.xhr("/json-rpc")
            .responseType("json")
            .header("Content-Type", "application/json")
            .post(JSON.stringify(rpcData),
                function (error, data) {
                    if (error) throw error;

                    var json = data.response.result;
                    force
                        .nodes(json.nodes)
                        .links(json.links)
                        .start();

                    var link = svg.selectall(".link")
                        .data(json.links)
                        .enter().append("line")
                        .attr("class", "link");

                    var node = svg.selectall(".node")
                        .data(json.nodes)
                        .enter().append("g")
                        .attr("class", "node")
                        .call(force.drag);

                    //node.append("image")
                    //    .attr("xlink:href", "https://github.com/favicon.ico")
                    //    .attr("x", -8)
                    //    .attr("y", -8)
                    //    .attr("width", 16)
                    //    .attr("height", 16);

                    node.append("text")
                        .attr("dx", 12)
                        .attr("dy", ".35em")
                        .text(function (d) {
                            return d.name
                        });

                    force.on("tick", function () {
                        link.attr("x1", function (d) {
                                return d.source.x;
                            })
                            .attr("y1", function (d) {
                                return d.source.y;
                            })
                            .attr("x2", function (d) {
                                return d.target.x;
                            })
                            .attr("y2", function (d) {
                                return d.target.y;
                            });

                        node.attr("transform", function (d) {
                            return "translate(" + d.x + "," + d.y + ")";
                        });
                    });
                });
    }

    // Links
    $(".xpm-resource-list .link").on("click", xpm.resource_action_callback);
    $(".xpm-resource-list a").on("click", xpm.resource_link_callback);
    $("#header").find(".links a").button();

    var click_state = function (e) {
        var checked = $(this).is(':checked');
        if (checked) {
            $("#resources").addClass(this.id);
        } else {
            $("#resources").removeClass(this.id);
        }
    };

    var statefilters = $("#state-chooser").find("li input");
    statefilters.button().on("click", click_state);
    statefilters.each(click_state);

    // Transform resource detailed view in tree
    $("#resource-detail-content").jstree();


// Create websocket
    var websocket_protocol = window.location.protocol == "https" ? "wss" : "ws";
    var websocket_url = websocket_protocol + "://" + window.location.host + "/web-socket";


    xpm.server = new $.JsonRpcClient({
        ajaxUrl: '/json-rpc',
        socketUrl: websocket_url,
        onmessage: xpm.handle_message,
        onopen: xpm.handle_ws_open,
        onclose: function () {
            noty({text: "Web socket closed", type: 'information', timeout: 2000});
            $("#connection").attr("src", "/images/disconnect.png").attr("alt", "[disconnected]");
            clearInterval(xpm.ping);
        }
    });

    // Get hostname and open the web socket
    xpm.server.call("hostname", {}, function (r) {
        // Set host name in title and header
        $("html head title").append("@" + r);
        $("#header").find("div.title").append("@" + r);
    });

    // Activate clibpoard copy
    $("#clipboard").click(function (event) {

        var node = event.target;
        if (node.localName == "dt") {
            node = <Element>node.nextSibling;
        }
        if (node.localName == "dd") {
            var range = document.createRange();
            range.selectNode(node);
            window.getSelection().addRange(range);


            if (document.execCommand('copy')) {
                noty({text: "Path " + range.toString() + " copied to clipboard", type: 'info', timeout: 5000});
            } else {
                noty({text: "Error: could not copy to clipboard", type: 'error', timeout: 5000});
            }
            window.getSelection().removeAllRanges();
        }

        $(this).dialog("close");
    });

    // Activate tabs
    $(".tab").tabs({
        beforeActivate: function (event, ui) {
            var tabid = ui.newPanel.attr("id");
            if (tabid == "experiments") {
                showexperiments(ui.newPanel);
            } else if (tabid == "xpm-info") {
                xpm.show_xpm_info();
            } else if (tabid == "xpm-help") {
                var select = $("#help-class-chooser");
                select.change(show_class_documentation);

                xpm.server.call("documentation.classes", {},
                    function (r) {
                        $.each(r, function (ix, e) {
                            select.append($e("option").text(e));
                        });
                    },
                    jsonrpc_error
                );
            }
        }
    });

})


