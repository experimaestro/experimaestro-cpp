/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
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

// Tests for JSON way of transmitting information


// START SNIPPET: task
var abc = new Namespace("a.b.c");
var task = {
    // The id of the task is an XML qualified name
    id: qname(ns, "task"),

    // One input of type xp:integer
    inputs: {
        x: { type: "xs:integer", default: 3 }
    }

    run: function(input) {
        return {
            "abc:output": {
                x: input.x,
                "" : []
            }
        };
    }
};

// Add the task to the list of available factories

xpm.add_task(task);

// END SNIPPET: task


/** Run and check */

// START SNIPPET: run
var task = xpm.get_task(ns, "task");
ns.x = 10;
var r = task.run();

// END SNIPPET: run

function test_json() {
	v = r.xp::value.@value;
	if (v == undefined || v != 10)
		throw new java.lang.String.format("Value [%s] is different from 10", v);
}

