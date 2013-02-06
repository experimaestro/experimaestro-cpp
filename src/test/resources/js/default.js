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

include("utils.inc.js");

// START SNIPPET: main
var abc = new Namespace("a.b.c");

var task = {
	id: qname("a.b.c", "task"),

	inputs: <inputs xmlns={xp.uri}>
	        <!-- a has a default value of 10 -->
            <value type="xs:integer" id="a" default="10"/>

            <!-- b has a default value given by the contained XML -->
            <value id="b" type="xs:integer">
                <default>20</default>
            </value>
        </inputs>,
};

xpm.add_task_factory(task);

var task = xpm.get_task(task.id);
var r = task.run();
// END SNIPPET: main


function test_default_attribute() {
	assert_equals(r.a, 10);
}

function test_default_element() {
	assert_equals(r.b, 20);
}	
