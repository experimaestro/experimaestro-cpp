package sf.net.experimaestro.connectors;

/**
 * sf.net.experimaestro.connectors
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class DefaultLauncher implements Launcher {
    @Override
    public XPMProcessBuilder processBuilder(SingleHostConnector connector) {
        return connector.processBuilder(connector);
    }
}
