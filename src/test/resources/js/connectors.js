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

var script_path = script_file().getParent().getName().getPath();
var repository_path = script_path + "/connectors.inc";
xpm.log("Repository path is " + repository_path);


/*
 
	Local connector

 */

function test_local() {
	include_repository("");
}


/*

	SSH connectors


*/

// Get the SSH port of the embedded SSH server

var port = sshd_server();
xpm.log("SSH server on port " + port);



// --- 


// --- Set the SSH options
var sshOptions = new ConnectorOptions("ssh");
sshOptions.useSSHAgent(true);


// --- One SSH host

function test_ssh() {
	var big_ssh = new SSHConnector("ssh://user@localhost", sshOptions);
	include_repository(big_ssh, script_path + ".inc");

	var irc = xpm.getTask(qname("test", "get-task"));
	irc.setParameter("x", "1");
	var result = irc.run();
}

// --- Use a group of machines
function test_ssh_group() {
	var ssh1 = new SSHConnector("localhost:223");
	var ssh2 = new SSHConnector("localhost:2132");

	var group = new ConnectorGroup(ssh1, ssh2);

	// Choose (randomly) one connector in the group, and parse the repository
	// that will be associated to all repositories
	include_repository(group, script_path + ".inc");

}