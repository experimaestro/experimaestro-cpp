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

/** Alternative 1 */

var configuration_alt_1 = {
	id: qname("a.b.c", "alt-1"),
	documentation: <p>Configuration of a possible alternative</p>,
	alternative: true,
	output: altName,
	
	inputs: <inputs>
		<value id="size" type="xs:integer" help="The parameter"/>
	</inputs>,
	
	outputs: <outputs>
		<output type="{a.b.c}alt"/>
	</outputs>,

	run: function(inputs) {
		return <alt xmlns="a.b.c">
				{inputs.size}
			</alt>;
	}
};

xpm.add_task_factory(configuration_alt_1);

/** Task */

var task_factory = {
	id: qname("a.b.c", "task"),
	version: "1.0",
	inputs: <inputs><alternative type="{a.b.c}alt" id="p"/></inputs>,
	
	run: function(inputs) {
		return <outputs>{inputs.p}</outputs>;
	}
};

xpm.add_task_factory(task_factory);

/** Run and output */

var task = xpm.get_task(task_factory.id);
task.setParameter("p", "{a.b.c}alt-1");
task.setParameter("p.size", "10");
var r = task.run();
xpm.log("Value of p.size is %s", r.abc::alt.xp::value.@value);

// END SNIPPET: main

function test_value() {
    v = r.abc::alt.xp::value.@value;
    if (v == undefined || v != 10)
    	throw new java.lang.String.format("Value [%s] is different from 10", r.abc::alt.xp::value.@value);
}
	
	