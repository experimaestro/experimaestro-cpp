/*
 *
 *  This file is part of experimaestro.
 *  Copyright (c) 2011 B. Piwowarski <benjamin@bpiwowar.net>
 *
 *  experimaestro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  experimaestro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package sf.net.experimaestro.tasks;

import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.xmlrpc.client.XmlRpcClient;

import sf.net.experimaestro.tasks.config.XMLRPCClientConfig;

import bpiwowar.argparser.Argument;
import bpiwowar.argparser.ArgumentClass;
import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;

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
