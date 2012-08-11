package sf.net.experimaestro.connectors;

/**
 * sf.net.experimaestro.connectors
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public interface Launcher {
    /**
     * Creates and returns a new process builder
     * @return A process builder
     */
    XPMProcessBuilder processBuilder(SingleHostConnector connector);
}
