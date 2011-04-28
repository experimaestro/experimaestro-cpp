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
  assert_equals("/a/b", xpm.xpath("xp:parentPath('/a/b/c.txt')", <a/>), "XPath function xp:parentPath");
}

function test_parentPath2() {
  assert_equals("/a/b", xpm.xpath("xp:parentPath(path)", <a><path>/a/b/c</path></a>), "XPath function xp:parentPath");
}


