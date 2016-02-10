package net.bpiwowar.xpm.server;

import net.bpiwowar.xpm.scheduler.Job;
import net.bpiwowar.xpm.scheduler.Message;

/**
 *
 */
public class ResourceProgressMessage extends Message {
    public ResourceProgressMessage(Job job) {
        super(Event.PROGRESS);
    }
}
