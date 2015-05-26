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

tasks.add("abc:default", {
	inputs: {
        a: { value: "xp:integer", default: 10 },
	}
});

// END SNIPPET: main

function test_default() {
    var r = tasks("abc:default").run({});
	assert_equals($(r[0]), 10);
}
