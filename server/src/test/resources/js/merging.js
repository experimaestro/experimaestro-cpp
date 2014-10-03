/*
 * Composed task (with parameter merging)
 *
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

// START SNIPPET: main

var abc = new Namespace("a.b.c");

// First task
tasks("abc:sub-task") = {
	inputs: { x: { value: "xp:integer" } }
};

// Third task
tasks("abc:task") = {
	/*
	    Connects the value returned by t1 to the input of x for t2
	*/
	inputs: {
	    subtask: { task: "abc:sub-task", merge: true}
	},
	
	run: function(inputs) {
		return inputs.subtask;
	}
		
};

// Get the task
var task = tasks("abc:task").create();
// Without merging, we would use "subtask.x"
task.set("x", 10);
var r = task.run();

// END SNIPPET: main

function test_merging() {
	if (r == undefined || _(r) != 10)
		throw new java.lang.String.format("Value [%s] is different from 10", _(r));
}
	
