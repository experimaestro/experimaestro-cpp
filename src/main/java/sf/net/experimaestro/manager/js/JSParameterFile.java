package sf.net.experimaestro.manager.js;

/**
 * A parameter file
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 24/1/13
 */
public class JSParameterFile extends JSBaseObject {
    String key;
    public byte[] value;

    public JSParameterFile(String key, byte[] value) {
        this.key = key;
        this.value = value;
    }

    public JSParameterFile(String key, JSBaseObject object) {
        this(key, object.getBytes());
    }

    public JSParameterFile(String key, String value) {
        this(key, value.getBytes());
    }

    @Override
    public String toString() {
        return key;
    }
}
