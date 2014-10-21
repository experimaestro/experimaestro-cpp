package sf.net.experimaestro.manager;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 18/1/13
 */
public interface ScriptRunner {
    /**
     * Evaluate the script and returns its output
     *
     * @param script
     * @return The output of the script - either as XML or as a String
     * @throws Exception if something goes wrong
     */
    Object evaluate(String script) throws Exception;
}
