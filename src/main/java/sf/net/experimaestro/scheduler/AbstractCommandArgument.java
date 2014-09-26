package sf.net.experimaestro.scheduler;

import com.sleepycat.persist.model.Persistent;

import java.util.Iterator;

/**
 * A command argument
 */
@Persistent
abstract public class AbstractCommandArgument {
    public static final AbstractCommandArgument.Pipe PIPE = new Pipe();

    @Persistent
    static public class Pipe extends AbstractCommandArgument {
        private Pipe() {}

        public static Pipe getInstance() {
            return PIPE;
        }
    }
}
