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


// START SNIPPET: main
var altName = xpm.qName("a.b.c", "alt");
var abc = new Namespace("a.b.c");


var task_1 = {
	id: xpm.qName("a.b.c", "task-1"),
	inputs: <inputs><input type="xs:integer" id="p"/></inputs>,
	outputs: <outputs><output type="xs:integer"/></outputs>,
	
	run: function(inputs) {
		return <outputs>{inputs.p}</outputs>;
	}
		
};

xpm.addTaskFactory(task_1);

var task_2 = {
	id: xpm.qName("a.b.c", "task-2"),
	inputs: <inputs xmlns:abc="a.b.c"><task type="abc:task-1" id="t1"/></inputs>,
	outputs: <outputs><output type="xs:integer"/></outputs>,
	
	run: function(inputs) {
		return <outputs>{inputs.t1.xp::value}</outputs>;
	}
		
};

xpm.addTaskFactory(task_2);

/** Run and check */

var task = xpm.getTask(task_2.id);
task.setParameter("t1.p", "10");
var r = task.run();

// END SNIPPET: main

v = r.xp::value.@value;
if (v == undefined || v != 10)
	throw new java.lang.String.format("Value [%s] is different from 10", r.abc::alt.xp::value.@value);
	
	
