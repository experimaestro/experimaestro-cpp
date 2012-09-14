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
var abc = new Namespace("a.b.c");

var task = {
	id: xpm.qName("a.b.c", "task"),

	inputs: <inputs xmlns={xp.uri}>
	        <!-- a has a default value of 10 -->
                <input type="xs:integer" id="a" default="10"/>

                <!-- b has a default value given by the contained XML -->
                <input id="b">
                    <default><value value="20"/></default>
                </input>

            </inputs>,
	outputs: <outputs><output id="a" type="xs:integer"/><output id="b" type="xs:integer"/></outputs>,
	
	run: function(inputs) {
		return <outputs>{inputs.a}{inputs.b}</outputs>;
	}
		
};

xpm.add_task_factory(task);

var task = xpm.getTask(task.id);
var r = task.run();
xpm.log("Output is %s", r);
// END SNIPPET: main

a = r.xp::value[0].@value;
if (a == undefined || a != 10)
	throw new java.lang.String.format("Value [%s] is different from 10", a);

b = r.xp::value[1].@value
if (b == undefined || b != 20)
	throw new java.lang.String.format("Value [%s] is different from 20", b);
	
	
