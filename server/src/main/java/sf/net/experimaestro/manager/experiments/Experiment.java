package sf.net.experimaestro.manager.experiments;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import org.apache.commons.vfs2.FileObject;
import sf.net.experimaestro.scheduler.Scheduler;

import java.util.ArrayList;

/**
 * An experiment
 */
@Entity
public class Experiment {
    /** Experiment identifier */
    @PrimaryKey
    String identifier;

    /** Working directory */
    FileObject workingDirectory;

    /** Tasks */
    ArrayList<Task> tasks = new ArrayList<>();

    /** Scheduler */
    private Scheduler scheduler;

    /**
     * New task
     * @param identifier The experiment identifier
     * @param workingDirectory The working directory for this experiment
     */
    public Experiment(String identifier, FileObject workingDirectory) {
        this.identifier = identifier;
        this.workingDirectory = workingDirectory;
    }

    public void init(Scheduler scheduler) {
        this.scheduler = scheduler;
    }
}
