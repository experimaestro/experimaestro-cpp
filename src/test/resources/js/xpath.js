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


/**
 * Test of XPath functions
 * (c) B. Piwowarski <benjamin@bpiwowar.net>
 */

var format = java.lang.String.format;
function assert_equals(expected, got, msg) {
    if (expected != got)
        throw format("%s: expected [%s] but got [%s]", msg, expected, got);
}

// --- Test the xp:parentPath function

function test_parentPath() {
  assert_equals("/a/b", xpath("xp:parentPath('/a/b/c.txt')", <a/>), "XPath function xp:parentPath");
}

function test_parentPath2() {
  assert_equals("/a/b", xpath("xp:parentPath(path)", <a><path>/a/b/c</path></a>), "XPath function xp:parentPath");
}


