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
var altName = qname("a.b.c", "alt");
var abc = new Namespace("a.b.c");


var task_1 = {
	id: qname("a.b.c", "task-1"),
	inputs: { p: { value: "xp:integer" } },
	
	run: function(inputs) {
		return inputs.p;
	}
		
};

xpm.add_task_factory(task_1);

var task_2 = {
	id: qname("a.b.c", "task-2"),
	inputs: { t1: { task: "abc:task-1" } },
	
	run: function(inputs) {
		return inputs.t1;
	}
		
};

xpm.add_task_factory(task_2);

/** Run and check */

var task = xpm.get_task(task_2.id);
task.set("t1.p", 10);
var r = task.run();

// END SNIPPET: main
if (r == undefined || r() != 10)
	throw new java.lang.String.format("Value [%s] is different from 10", r);
	
	
