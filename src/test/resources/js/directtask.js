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

/** Direct task */

// START SNIPPET: task
var abc = Namespace("a.b.c");
tasks("abc:task") = {
    // One input of type xp:integer
    inputs: { x: { value: "xp:integer" } }
};
// END SNIPPET: task


/** Run and check */

// START SNIPPET: run
var r = tasks("abc:task").run({x: 10})[0];
// END SNIPPET: run

function test_directtask() {
	if (r == undefined || r() != 10)
		throw new java.lang.String.format("Value [%s] is different from 10", r);
}	
	
