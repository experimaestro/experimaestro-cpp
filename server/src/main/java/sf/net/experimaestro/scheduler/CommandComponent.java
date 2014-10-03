package sf.net.experimaestro.scheduler;

import java.io.IOException;
import java.util.stream.Stream;

/**
 * A command component that can be processed depending on where the command is running.
 * <p/>
 * This is used e.g. when there is a path that has to be transformed because the running host
 * has a different path mapping than the host where the command line was configured.
 * <p/>
 * It is the concatenation of
 * <ul>
 * <li>strings</li>
 * <li>paths</li>
 * </ul>
 * <p/>
 * Paths can be localized depending on where the command is run.
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public interface CommandComponent {

    /**
     * Returns the path to the file of this component
     *
     * @param environment Binds identifiers to file objects
     * @return A string representing the path to the file for this component
     * @throws org.apache.commons.vfs2.FileSystemException
     */
    String prepare(CommandEnvironment environment) throws IOException;

    default Stream<? extends CommandComponent> allComponents() {
        return Stream.of(this);
    }

}
