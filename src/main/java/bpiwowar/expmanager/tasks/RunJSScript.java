package bpiwowar.expmanager.tasks;

import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.xmlrpc.client.XmlRpcClient;

import bpiwowar.argparser.Argument;
import bpiwowar.argparser.ArgumentClass;
import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;
import bpiwowar.expmanager.tasks.config.XMLRPCClientConfig;

@TaskDescription(name = "run-js-script", project = { "xpmanager" })
public class RunJSScript extends AbstractTask {
	@ArgumentClass(prefix = "xmlrpc", help = "Configuration file for the XML RPC call", required = true)
	XMLRPCClientConfig xmlrpcClientConfig;

	@Argument(name = "script", help = "The script to execute (null for standard input)")
	File file;

	@Override
	public int execute() throws Throwable {
		// Connection to the server
		XmlRpcClient client = xmlrpcClientConfig.getClient();

		// Read the file

		// Execute on the server
		ArrayList<Object> params = new ArrayList<Object>();
		

		if (file == null) {
			// Read the script from standard input
			InputStreamReader in = new InputStreamReader(System.in);
			StringBuffer sb = new StringBuffer();
			int len = 0;
			char[] buffer = new char[8192];
			while ((len = in.read(buffer)) >= 0) 
				sb.append(buffer, 0, len);

			// Parameters for the server
			params.add(false);
			params.add(sb.toString());
		} else {
			params.add(true);
			params.add(file.getAbsolutePath());
		}

		params.add(System.getenv());
		Boolean returns = (Boolean) client.execute("TaskManager.runJSScript",
				params.toArray());

		return returns ? 0 : 1;
		
	}

}
