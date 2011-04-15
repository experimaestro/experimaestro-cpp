package sf.net.experimaestro.tasks;

import java.io.File;
import java.util.ArrayList;

import org.apache.xmlrpc.client.XmlRpcClient;

import sf.net.experimaestro.rsrc.LockMode;
import sf.net.experimaestro.tasks.config.XMLRPCClientConfig;
import sf.net.experimaestro.utils.GenericHelper;

import bpiwowar.argparser.Argument;
import bpiwowar.argparser.ArgumentClass;
import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;

/**
 * Create a simple data resource
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@TaskDescription(name="create-data", project = { "xpmanager" })
public class CreateData extends AbstractTask {
	@ArgumentClass(prefix = "xmlrpc", help = "Configuration file for the XML RPC call", required = true)
	XMLRPCClientConfig xmlrpcClientConfig;

	@Argument(name = "basename", required = true, help = "Basename for the resource")
	File basename;

	@Argument(name = "mode", help = "The access mode of the resource")
	LockMode mode;

	@Argument(name = "exists", help = "The resource already exists")
	boolean exists;

	@Override
	public int execute() throws Throwable {
		// Contact the XML RPC server
		XmlRpcClient client = xmlrpcClientConfig.getClient();

		ArrayList<Object> params = GenericHelper.newArrayList();
		params.add(basename.getAbsoluteFile().toString());
		params.add(mode.toString());
		params.add(exists);

		Boolean returns = (Boolean) client.execute("TaskManager.addData",
				params.toArray());
		
		return returns ? 0 : 1;
	}
}
