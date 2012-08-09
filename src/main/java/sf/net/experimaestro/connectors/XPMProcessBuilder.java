package sf.net.experimaestro.connectors;

import sf.net.experimaestro.utils.arrays.ListAdaptator;

import java.util.List;

/**
 * This class mimics java.lang.ProcessBuilder to offer a more generic way of
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
abstract public class XPMProcessBuilder {
    /** The command to run */
    private List<String> command;

    abstract public XPMProcessBuilder command(List<String> command) {
        this.command = command;
    }

    public XPMProcessBuilder command(String... command) {
        return command(ListAdaptator.create(command));
    }

    public List<String> command() {
        return command;
    }

    public XPMProcess start() {

    }
}
