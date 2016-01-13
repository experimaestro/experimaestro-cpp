package net.bpiwowar.xpm.manager.scripting;

import org.apache.commons.lang.mutable.MutableInt;
import net.bpiwowar.xpm.exceptions.XPMAssertionError;
import net.bpiwowar.xpm.manager.plans.Operator;
import net.bpiwowar.xpm.scheduler.Resource;
import net.bpiwowar.xpm.utils.Updatable;

import java.util.HashMap;
import java.util.Map;

/**
 * A context used when running javascript
 */
public class RunningContext implements AutoCloseable {
    final static private ThreadLocal<RunningContext> threadContext = new ThreadLocal<>();

    /**
     * Parent
     */
    RunningContext parent;

    /**
     * Counts the number of items output by an operator; null if not used
     */
    private Map<Operator, MutableInt> counts;

    private Map<String, Resource> submittedJobs;

    /**
     * Whether we are simulating
     */
    Updatable<Boolean> simulate;

    public RunningContext() {
        if (threadContext.get() == null) {
            threadContext.set(this);
            simulate = new Updatable<>(false);
            counts = null;
            submittedJobs = new HashMap<>();
        } else {
            parent = threadContext.get();

            counts = parent.counts;
            simulate = parent.simulate.reference();
            submittedJobs = parent.submittedJobs;

            threadContext.set(this);
        }

    }

    public RunningContext counts(boolean flag) {
        if (flag) counts = new HashMap<>();
        else counts = null;
        return this;
    }

    public Map<Operator, MutableInt> counts() {
        return counts;
    }

    public boolean simulate() {
        return simulate.get();
    }

    public RunningContext simulate(boolean simulate) {
        this.simulate.set(simulate);
        return this;
    }


    public static RunningContext get() {
        return threadContext.get();
    }

    @Override
    public void close() {
        if (this != threadContext.get()) {
            throw new XPMAssertionError("Running context do not match current");
        }

        threadContext.set(parent);
    }

    /**
     * Submitted jobs
     */
    public Map<String, Resource> getSubmittedJobs() {
        return submittedJobs;
    }

}
