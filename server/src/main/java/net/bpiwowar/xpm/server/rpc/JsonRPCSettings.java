package net.bpiwowar.xpm.server.rpc;

import net.bpiwowar.xpm.manager.Repositories;
import net.bpiwowar.xpm.scheduler.Scheduler;
import net.bpiwowar.xpm.server.ServerSettings;
import org.eclipse.jetty.server.Server;

/**
 *
 */
public class JsonRPCSettings {
    final Scheduler scheduler;
    final Repositories repository;
    private final Server server;
    final ServerSettings serverSettings;

    public JsonRPCSettings(Scheduler scheduler, Repositories repository, Server server, ServerSettings serverSettings) {
        this.scheduler = scheduler;
        this.repository = repository;
        this.server = server;
        this.serverSettings = serverSettings;
    }
}
