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

// Declares the alternative
var altName = qname("a.b.c", "alt");
var abc = new Namespace("a.b.c");

xpm.declare_alternative(altName);

/** Alternative */
tasks.set("abc:alt1", {
	documentation: <p>"Configuration of a possible alternative"</p>,
	alternative: altName,
	
	inputs: {
	    x: { value: "xp:integer", help: "The parameter" }
	},
	
	run: function(inputs) {
		return { "x": inputs.x, "$$type": "abc:alt" };
	}
});

/** Task */

tasks.set("abc:task", {
	inputs: {
	    p: { alternative: "abc:alt" }
	},
	
	run: function(inputs) {
		return inputs.p;
	}
});

// END SNIPPET: main

function test_value() {
    var task = tasks.get("abc:task").create();
    
    task.set("p", "{a.b.c}alt1");
    task.set("p.x", 10);
    
    var r = task.run();
    var v = _(r.x);

    if (typeof(v) == "undefined") 
        v = null;
    if (v != 10)
    	throw new java.lang.String.format("Value [%s] is different from 10", v);
}
	
	