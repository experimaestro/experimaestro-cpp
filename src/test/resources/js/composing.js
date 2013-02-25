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

var task_1 = {
	id: qname("a.b.c", "task-1"),
	inputs: <inputs xmlns={xp.uri}><value type="xs:integer" id="x"/></inputs>,		
};

var task_2 = {
	id: qname("a.b.c", "task-2"),
	inputs: <inputs xmlns={xp.uri}><value type="xs:integer" id="x"/></inputs>,	
};

var task = {
	id: qname("a.b.c", "task"),
	inputs: 
        <inputs xmlns:abc="a.b.c" xmlns:xp={xp.uri} xmlns={xp.uri}>
            <task ref="abc:task-2" id="t2">
                <connect from="t1.x" path="." to="x"/>
            </task>
            
            <task ref="abc:task-1" id="t1"/>
        </inputs>,
	
	run: function(inputs) {
		return inputs.t2.x;
	}
		
};

/* 
TODO: ease the composition by
tasks.abc::task = compose({
    [tasks.abc::task_2, { x: tasks.abc.task_2::x } ]
});
*/


// Add tasks

xpm.add_task_factory(task_1);
xpm.add_task_factory(task_2);
xpm.add_task_factory(task);

// Run and check

var task = xpm.get_task(task.id);
task.setParameter("t1.x", "10");
var r = task.run();

// END SNIPPET: main
function test_value() {
    if (r == undefined || r != 10)
    	throw new java.lang.String.format("Value [%s] is different from 10", r);
}
	
