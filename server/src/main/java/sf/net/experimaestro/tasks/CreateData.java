package sf.net.experimaestro.tasks;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
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

import bpiwowar.argparser.Argument;
import bpiwowar.argparser.ArgumentClass;
import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;
import org.apache.xmlrpc.client.XmlRpcClient;
import sf.net.experimaestro.scheduler.LockMode;
import sf.net.experimaestro.tasks.config.XMLRPCClientConfig;
import sf.net.experimaestro.utils.GenericHelper;

import java.io.File;
import java.util.ArrayList;

/**
 * Create a simple data resource
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@TaskDescription(name = "create-data", project = {"xpmanager"})
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
