package bpiwowar.expmanager.tasks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;

import bpiwowar.argparser.ArgParseException;
import bpiwowar.argparser.ArgParser;
import bpiwowar.argparser.Argument;
import bpiwowar.argparser.ArgumentClass;
import bpiwowar.argparser.IllegalArgumentValue;
import bpiwowar.expmanager.rsrc.LockMode;
import bpiwowar.expmanager.tasks.config.XMLRPCClientConfig;
import bpiwowar.utils.GenericHelper;

/**
 * Create a simple data resource
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class CreateData {
	@ArgumentClass(prefix = "xmlrpc", help = "Configuration file for the XML RPC call", required = true)
	XMLRPCClientConfig xmlrpcClientConfig;

	@Argument(name = "basename", required = true, help = "Basename for the resource")
	File basename;

	@Argument(name = "mode", help = "The access mode of the resource")
	LockMode mode;

	@Argument(name = "exists", help = "The resource already exists")
	boolean exists;

	public CreateData(String[] args) throws IllegalArgumentValue, IOException,
			ArgParseException, XmlRpcException {
		// Read the arguments
		ArgParser argParser = new ArgParser("create-job");
		argParser.addOptions(this);
		argParser.matchAllArgs(args);

		// Contact the XML RPC server
		XmlRpcClient client = xmlrpcClientConfig.getClient();

		ArrayList<Object> params = GenericHelper.newArrayList();
		params.add(basename.getAbsoluteFile().toString());
		params.add(mode.toString());
		params.add(exists);

		Boolean returns = (Boolean) client.execute("TaskManager.addData",
				params.toArray());
		if (!returns)
			throw new RuntimeException();

	}
}
