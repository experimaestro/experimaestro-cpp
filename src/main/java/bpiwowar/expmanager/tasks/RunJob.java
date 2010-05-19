package bpiwowar.expmanager.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import bpiwowar.argparser.ArgParseException;
import bpiwowar.argparser.ArgParser;
import bpiwowar.argparser.ArgParserOption;
import bpiwowar.argparser.Argument;
import bpiwowar.argparser.ArgumentRegexp;
import bpiwowar.argparser.IllegalArgumentValue;
import bpiwowar.log.Logger;
import bpiwowar.utils.GenericHelper;

/**
 * Add a job to the list of jobs to be executed - if the job has already been
 * executed, this only register the job
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class RunJob {
	final static private Logger LOGGER = Logger.getLogger();

	@Argument(name = "xmlrpc", help = "Configuration file for the XML RPC call", required = true)
	File xmlrpcfile;

	@Argument(name = "basename", help = "Basename for the resource", required = true)
	File basename;

	/**
	 * Parameters associated to this
	 */
	Map<String, String> parameters = GenericHelper.newTreeMap();

	@Argument(name = "param", required = false)
	@ArgumentRegexp("^([^=])=(.*)$")
	void addParameter(String name, String value) {
		parameters.put(name, value);
	}

	@Argument(name = "prority", help = "Priority (higher for more urgent tasks, 0 by default)")
	int priority = 0;
	
	@Argument(name = "depends", help = "List of jobs on which we depend")
	TreeSet<String> depends = GenericHelper.newTreeSet();

	@Argument(name = "lock-write", help = "Lock for read/write", required = false)
	TreeSet<String> writeLocks = GenericHelper.newTreeSet();

	@Argument(name = "lock-read", help = "Lock for read", required = false)
	TreeSet<String> readLocks = GenericHelper.newTreeSet();

	@Argument(name = "data", required = false, help = "Produced data")
	void addData(File file) {

	}

	public RunJob(String[] args) throws IllegalArgumentValue, IOException,
			ArgParseException, XmlRpcException {
		// Read the arguments
		ArgParser argParser = new ArgParser("create-job");
		argParser.addOptions(this);
		String[] command = argParser.matchAllArgs(args, 0,
				ArgParserOption.STOP_FIRST_UNMATCHED,
				ArgParserOption.EXCEPTION_ON_ERROR);

		if (command == null || command.length == 0)
			throw new RuntimeException("There should be a command to run");

		// Contact the XML RPC server

		Properties xmlrpcConfig = new Properties();
		xmlrpcConfig.load(new FileInputStream(xmlrpcfile));

		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		final String url = xmlrpcConfig.getProperty("url");
		LOGGER.info("XML RPC server is at %s", url);
		config.setServerURL(new URL(url));

		if (xmlrpcConfig.contains("user")) {
			config.setBasicUserName(xmlrpcConfig.getProperty("user"));
			config.setBasicPassword(xmlrpcConfig.getProperty("password"));
		}

		XmlRpcClient client = new XmlRpcClient();
		client.setConfig(config);

		// Let's go
		ArrayList<Object> params = GenericHelper.newArrayList();
		params.add(basename.toString());
		params.add(priority);
		params.add(command);
		params.add(depends);
		params.add(readLocks.toArray());
		params.add(writeLocks.toArray());

		Boolean returns = (Boolean) client.execute("TaskManager.runCommand", params
				.toArray());
		if (!returns)
			throw new RuntimeException();
	}
}
