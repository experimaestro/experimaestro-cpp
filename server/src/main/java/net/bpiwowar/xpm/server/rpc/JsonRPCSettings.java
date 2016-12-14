package net.bpiwowar.xpm.server.rpc;

import net.bpiwowar.xpm.scheduler.Scheduler;
import net.bpiwowar.xpm.server.ServerSettings;
import org.eclipse.jetty.server.Server;

/**
 *
 */
public class JsonRPCSettings {
    final Scheduler scheduler;
    final Server server;
    final ServerSettings serverSettings;

    public JsonRPCSettings(Scheduler scheduler, Server server, ServerSettings serverSettings) {
        this.scheduler = scheduler;
        this.server = server;
        this.serverSettings = serverSettings;
    }
}
