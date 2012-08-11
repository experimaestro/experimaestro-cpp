



// --- Set the SSH options
var sshOptions = new ConnectorOptions("ssh");
sshOptions.useSSHAgent(true);

var big_ssh = new SSHConnector("ssh://bpiwowar@big.lip6.fr", sshOptions);


// --- One SSH host

if (false) {
	xpm.includeRepository(big_ssh, "/home/bpiwowar/ircollections/etc/irc.js");
	var irc = xpm.getTask(qname("http://ircollections.sourceforge.net", "get-task"));
	irc.setParameter("id", "trec.1/adhoc");
	xpm.log(irc.run());
}

// --- Use a group of machines
if (false) {
	var tiger_ssh = new SSHConnector("tiger.lip6.fr");
	var jellyfish_ssh = new SSHConnector("jellyfish.lip6.fr");

	var group = new ConnectorGroup(tiger_ssh, jellyfish_ssh);

	// Choose (randomly) one connector in the group, and parse the repository
	// that will be associated to all repositories
	includeRepository(group, "/home/bpiwowar/ircollections/etc/irc.js");

	var irc = xpm.getTask(qname("http://ircollections.sourceforge.net", "get-task"));
	irc.setParameter("id", "trec.1/adhoc");
	xpm.log(irc.run());
}