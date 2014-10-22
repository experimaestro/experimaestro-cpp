package sf.net.experimaestro.manager.experiments;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityStore;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.scheduler.CachedEntitiesStore;
import sf.net.experimaestro.scheduler.Scheduler;

/**
 * Experiments store
 */
public class Tasks extends CachedEntitiesStore<QName, Task> {
    private final Scheduler scheduler;

    /**
     * Initialise the set of values
     *
     * @param dbStore the database store
     * @throws com.sleepycat.je.DatabaseException If something goes wrong
     */
    public Tasks(Scheduler scheduler, EntityStore dbStore) throws DatabaseException {
        super(dbStore.getPrimaryIndex(QName.class, Task.class));
        this.scheduler = scheduler;
    }

    @Override
    protected boolean canOverride(Task old, Task task) {
        return false;
    }

    @Override
    protected QName getKey(Task task) {
        return task.identifier;
    }

    @Override
    protected void init(Task task) {

    }
}
