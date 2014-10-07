package sf.net.experimaestro.manager.js.object;

import org.mozilla.javascript.Wrapper;
import sf.net.experimaestro.manager.js.JSBaseObject;
import sf.net.experimaestro.manager.js.JSObjectDescription;
import sf.net.experimaestro.scheduler.Command;

/**
 * Just a pipe
 */
@JSObjectDescription(name = "Pipe")
public class Pipe extends JSBaseObject implements Wrapper {
    @Override
    public Object unwrap() {
        return Command.Pipe.getInstance();
    }
}
