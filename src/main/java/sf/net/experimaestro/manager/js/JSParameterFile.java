package sf.net.experimaestro.manager.js;

/**
 * A parameter file
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 24/1/13
 */
public class JSParameterFile extends JSBaseObject {
    String key;
    public String value;

    public JSParameterFile(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return key;
    }
}
