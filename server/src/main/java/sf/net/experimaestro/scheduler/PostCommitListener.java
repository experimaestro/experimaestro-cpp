package sf.net.experimaestro.scheduler;

/**
 * Post commit listener
 */
public interface PostCommitListener {
    default void postCommit(Transaction transaction) {}
}
