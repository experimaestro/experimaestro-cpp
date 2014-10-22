package sf.net.experimaestro.manager.experiments;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityStore;
import sf.net.experimaestro.scheduler.CachedEntitiesStore;
import sf.net.experimaestro.scheduler.Scheduler;

/**
 * Experiments store
 */
public class Experiments extends CachedEntitiesStore<String, Experiment> {
    private final Scheduler scheduler;

    /**
     * Initialise the set of values
     *
     * @param dbStore the database store
     * @throws com.sleepycat.je.DatabaseException If something goes wrong
     */
    public Experiments(Scheduler scheduler, EntityStore dbStore) throws DatabaseException {
        super(dbStore.getPrimaryIndex(String.class, Experiment.class));
        this.scheduler = scheduler;
    }

    @Override
    protected boolean canOverride(Experiment old, Experiment experiment) {
        return false;
    }

    @Override
    protected String getKey(Experiment experiment) {
        return experiment.identifier;
    }

    @Override
    protected void init(Experiment experiment) {
        experiment.init(scheduler);
    }
}
