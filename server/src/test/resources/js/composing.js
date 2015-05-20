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


/*
 * Example of a composed task
 *
 * (c) B. Piwowarski, 2010
 */

// START SNIPPET: main

var abc = new Namespace("a.b.c");

tasks.add("abc:task-1", {
	inputs: { x: { value: "xp:integer" } }
});

tasks.add("abc:task-2", {
	inputs: { x: { value: "xp:integer"} }
});

tasks.add("abc:task", {
	inputs: {
	    t1: { task: "abc:task-1" },
        t2: { task: "abc:task-2", 
            connect: { x: function(t1) { return t1; } } 
        }
    },

	run: function(inputs) {
		return $(inputs.t2);
	}
		
});


// Run and check

var r = tasks("abc:task").run({"t1.x": 10})[0];

// END SNIPPET: main
function test_value() {
    v = typeof(r) == "undefined" ? null : _(r);
    if (v != 10)
    	throw new java.lang.String.format("Value [%s] is different from 10", v);
}
