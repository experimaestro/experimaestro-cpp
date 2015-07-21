/*
 *
 *  * This file is part of experimaestro.
 *  * Copyright (c) 2015 B. Piwowarski <benjamin@bpiwowar.net>
 *  *
 *  * experimaestro is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * experimaestro is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

// Get script path

var script_path = script_file().get_parent();
var repository_path = script_path.path("connectors.inc.js");

function assert_true(r) {
    if (!r) {
        throw java.lang.String.format("Test did not return true");
    }
}

// Check the answer
function check(r) {
    var v = $(r);
    var expected = "experimaestro rocks";
    if (v != expected) {
        throw new java.lang.String.format("Value [%s] is different from [%s]", v, expected);
    }
}

/*

 Local connector

 */

function test_local() {
    include_repository(repository_path);
    var task = xpm.get_task("a.b.c", "task");
    task.set("x", 10);
    var r = task.run();
    check(r);
}

/*

 SSH connectors


 */

function get_ssh_connector() {
    var port = sshd_server();
    logger.info("SSH server on port " + port);

    var sshOptions = SSHOptions();
    sshOptions.password("user");
    sshOptions.check_host(false);
    sshOptions.set_use_ssh_agent(false);
    return Connector.create("ssh://user@localhost" + ":" + port, sshOptions);
}


// Test an SSH share
function test_share() {
    var server = get_ssh_connector();
    define_share("test", "root", server, "/");
    var p = path("shares:test:root:" + repository_path.get_ancestor(2).resolve("hello").get_path());
    var s = p.read_all();
    logger.info("Read (share): [%s]", s);
    assert_true(s == "world\n");
}

// Test resolution of a shared volume
function test_share_resolution() {
    var server = get_ssh_connector();
    define_share("test", "root", server, "/");
    var p = path("shares:test:root:" + repository_path.get_ancestor(2).resolve("hello").get_path());
    var s = xpm.evaluate(["/bin/cat", p], { launcher: server.default_launcher() });
    logger.info("Read (share resolution): [%s]", s);
    assert_true(s == "world");
}

// Test
function disabled_test_ssh_repository() {
    var server = get_ssh_connector();
    include_repository(server, repository_path.get_path());
    logger.info("Included SSH repository");

    var task = tasks("{a.b.c}task").create();
    task.set("x", 10);
    var r = task.run();
    check(r);
}

// --- Use a group of machines (disabled)
function disabled_test_ssh_group() {
    var ssh1 = new Connector("ssh://localhost:223");
    var ssh2 = new Connector("ssh://localhost:2132");

    var group = new ConnectorGroup(ssh1, ssh2);

    // Choose (randomly) one connector in the group, and parse the repository
    // that will be associated to all repositories
    include_repository(group, repository_path);

}