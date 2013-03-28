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


// custom css expression for a case-insensitive contains()
jQuery.expr[':'].Contains = function(a,i,m){
  return (a.textContent || a.innerText || "").toUpperCase().indexOf(m[3].toUpperCase())>=0;
};


  function listFilter(list) { // header is any element, list is an unordered list
    // create and add the filter form to the header
    var form = $("<form>").attr({"class":"filterform","action":"javascript:void(0)"}),
        input = $("<input>").attr({"class":"filterinput","type":"text"});
    $(form).append(input).prependTo(list);

    $(input)
      .change( function () {
        var filter = $(this).val();
        if(filter) {
          // this finds all links in a list that contain the input,
          // and hide the ones not containing the input while showing the ones that do
          $(list).find("a:not(:Contains(" + filter + "))").parent().slideUp();
          $(list).find("a:Contains(" + filter + ")").parent().slideDown();
        } else {
          $(list).find("li").slideDown();
        }
        return false;
      })
    .keyup( function () {
        // fire the above change event after every letter
        $(this).change();
    });
  }


$().ready(function() {


    $(".xpm-resource-list").each(function() { listFilter($(this)); });

    // Tabs as tabs
    $( ".tab" ).tabs();
    $( "#resources" ).children("div").each(function() {
        var n = $(this).children("ul").children("li").size();
        var tabid = $(this).attr("id");
        $("#" + tabid + "-count").text(n);
    });

    $(".resource").each(function(r) {
        var rId = $(this).attr("name");
    });
});