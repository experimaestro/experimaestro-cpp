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
tasks("abc:sub_task") = {
	inputs: <inputs><value type="xs:integer" id="x"/></inputs>,	
};

// Third task
tasks("abc:task") = {
	/*
	    Connects the value returned by t1 to the input of x for t2
	*/
	inputs:
        <inputs xmlns:abc="a.b.c" xmlns:xp={xp.uri} xmlns={xp.uri}>
            <task ref="abc:sub_task" id="subtask" merge="true"/>
        </inputs>,
	
	run: function(inputs) {
		return inputs.subtask;
	}
		
};

// Get the task
var task = tasks("abc:task").create();
// Without merging, we would use "subtask.x"
task.set("x", "10");
// <outputs>10</outputs>
var r = task.run();

// END SNIPPET: main

function test_composing_2() {
    r = r.get_value("x");
	if (r == undefined || r != 10)
		throw new java.lang.String.format("Value [%s] is different from 10", r);
}
	
