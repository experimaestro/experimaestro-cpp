package net.bpiwowar.xpm.manager.experiments;

import net.bpiwowar.xpm.scheduler.Resource;
import net.bpiwowar.xpm.scheduler.ResourceMessage;

/**
 *
 */
public class ExperimentResourceAddedMessage extends ExperimentMessage {
    private final String taskId;
    ResourceMessage resource;

    public ExperimentResourceAddedMessage(TaskReference taskReference, Resource resource) {
        super(Event.EXPERIMENT_RESOURCE_ADDED, taskReference.experiment);
        this.taskId = taskReference.getTaskId().toString();
        this.resource = ResourceMessage.added(resource);
    }
}
