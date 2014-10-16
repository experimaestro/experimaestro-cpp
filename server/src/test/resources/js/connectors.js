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

// Get script path

var script_path = script_file().get_parent();
var repository_path = script_path.path("connectors.inc.js");


// Check the answer
function check(r) {
	var v = _(r);
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


// --- One SSH host

function test_ssh() {
	var sshOptions = SSHOptions();
	sshOptions.password = "user";

	var port = sshd_server();
	logger.info("SSH server on port " + port);

	var big_ssh = new Connector("ssh://user@localhost" + ":" + port);
	include_repository(big_ssh, repository_path.get_path());
    logger.info("Included repository");

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