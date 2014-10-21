package sf.net.experimaestro.scheduler;

/**
 * Defines classes that can clean themselves before being
 * deleted from database
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 25/1/13
 */
public interface Cleaneable {
    /**
     * Clean before deletion
     */
    void clean();

}
