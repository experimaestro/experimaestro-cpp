package sf.net.experimaestro.manager.js;

import org.mozilla.javascript.Wrapper;
import sf.net.experimaestro.scheduler.Dependency;

/**
 * Created by bpiwowar on 11/9/14.
 */
public class JSDependency extends JSBaseObject implements Wrapper {
    /**
     * The wrapped dependency
     */
    private Dependency dependency;

    @JSFunction
    public JSDependency(Dependency dependency) {
        this.dependency = dependency;
    }

    @Override
    public String toString() {
        return dependency.toString();
    }

    @Override
    public Object unwrap() {
        return dependency;
    }
}
