package sf.net.experimaestro.scheduler;

import java.util.function.Consumer;

/**
 * Post commit listener
 */
public interface PostCommitListener extends Consumer<Transaction> {
}
