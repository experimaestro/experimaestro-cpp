package sf.net.experimaestro.manager.experiments;

import java.nio.file.Path;
import sf.net.experimaestro.annotations.Exposed;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.jpa.PathConverter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;

/**
 * An experiment
 */
@Entity
@Exposed
public class Experiment {
    /** Experiment taskId */
    @Id
    long id;

    /** Tasks */
    @ManyToMany(fetch = FetchType.LAZY)
    Collection<TaskReference> tasks = new ArrayList<>();

    /** Working directory */
    @Convert(converter = PathConverter.class)
    Path workingDirectory;

    /** Timestamp */
    private final long timestamp;

    /** Identifier */
    String identifier;

    /** Scheduler */
    transient private Scheduler scheduler;

    /**
     * New task
     * @param identifier The experiment taskId
     * @param workingDirectory The working directory for this experiment
     */
    public Experiment(String identifier, long timestamp, Path workingDirectory) {
        this.identifier = identifier;
        this.timestamp = timestamp;
        this.workingDirectory = workingDirectory;
    }

    public void init(Scheduler scheduler) {
        this.scheduler = scheduler;
    }
}
