package sf.net.experimaestro.scheduler;

import com.sleepycat.je.Transaction;

import java.util.ArrayList;

/**
 * Container for database transactions, able to perform some extra cleanup
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 27/1/13
 */
final public class XPMTransaction implements AutoCloseable {

    private final Transaction transaction;
    private boolean closed = false;

    /**
     * To be run when the transaction is commited
     */
    private ArrayList<Runnable> commitActions = new ArrayList<>();

    public XPMTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    static Transaction transaction(XPMTransaction txn) {
        return txn == null ? null : txn.getTransaction();
    }

    public void commit() {
        transaction.commit();
        for (Runnable runnable : commitActions)
            runnable.run();
        closed = true;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    @Override
    public void close() {
        if (!closed) {
            transaction.abort();
            closed = true;
        }
    }

    public void abort() {
        transaction.abort();
        closed = true;
    }

    public void addCommitAction(Runnable runnable) {
        commitActions.add(runnable);
    }
}
