/*
 * This file is part of experimaestro.
 * Copyright (c) 2015 B. Piwowarski <benjamin@bpiwowar.net>
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

var ns = new Namespace("xpm.tests");

tasks.add("ns:mult", {
    inputs: {
    },

    run: function(x) {
        return parameters("hello");
    }
});

function test_parameter() {
    var r = tasks("ns:mult")
        .plan({})
        .parameters({hello: "world"})
        .run();

    // Check that the context was world
    if ($(r[0]) != "world") {
        throw new Error(format("Parameter hello is not world but %s", $(r[0])));
    }

    // Check that global context is
    var v = parameters("hello");
    if (typeof(v) != "undefined") {
        throw new Error(format("Parameter hello is not undefined but %s", v));
    }

}
