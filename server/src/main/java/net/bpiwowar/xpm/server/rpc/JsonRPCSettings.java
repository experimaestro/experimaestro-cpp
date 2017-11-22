package net.bpiwowar.xpm.server.rpc;

import net.bpiwowar.xpm.scheduler.Scheduler;
import net.bpiwowar.xpm.tasks.ServerCommand;
import org.eclipse.jetty.server.Server;

/**
 *
 */
public class JsonRPCSettings {
    final Scheduler scheduler;
    final Server server;
    final ServerCommand.ServerSettings serverSettings;

    public JsonRPCSettings(Scheduler scheduler, Server server, ServerCommand.ServerSettings serverSettings) {
        this.scheduler = scheduler;
        this.server = server;
        this.serverSettings = serverSettings;
    }
}
