package net.bpiwowar.xpm.manager.experiments;

import net.bpiwowar.xpm.scheduler.Message;

/**
 *
 */
public class ExperimentMessage extends Message {
    private final long id;
    private final String name;
    private final long timestamp;

    public ExperimentMessage(Event event, Experiment experiment) {
        super(event);
        this.name = experiment.getName();
        this.id = experiment.getId() != null ? experiment.getId() : -1;
        this.timestamp = experiment.getTimestamp();
    }
}
