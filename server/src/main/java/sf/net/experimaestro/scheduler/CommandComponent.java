package sf.net.experimaestro.scheduler;

import org.apache.commons.vfs2.FileSystemException;

import java.io.IOException;
import java.util.function.Consumer;
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
     * @param environment The command environment
     * @return A string representing the path to the file for this component, or null if this
     * command component has no direct string representation
     * @throws org.apache.commons.vfs2.FileSystemException
     */
    default String toString(CommandContext environment) throws IOException {
        return null;
    }

    default Stream<? extends CommandComponent> allComponents() {
        return Stream.of(this);
    }

    /** Prepare the command to be written */
    default void prepare(CommandContext environment) throws FileSystemException {}

    default void forEachCommand(Consumer<? super AbstractCommand> consumer) {}
}
