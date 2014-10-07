package sf.net.experimaestro.manager.json;

import sf.net.experimaestro.manager.QName;

import java.io.IOException;
import java.io.Writer;

/**
 * Created by bpiwowar on 10/9/14.
 */
public class JsonNull implements Json {
    private static JsonNull singleton = new JsonNull();

    private JsonNull() {}

    public static JsonNull getSingleton() {
        return singleton;
    }


    @Override
    public Json clone() {
        return this;
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public Object get() {
        return null;
    }

    @Override
    public QName type() {
        return null;
    }

    @Override
    public boolean canIgnore(JsonWriterOptions options) {
        return true;
    }

    @Override
    public void writeDescriptorString(Writer writer, JsonWriterOptions options) throws IOException {
        writer.write("null");
    }

    @Override
    public void write(Writer out) throws IOException {
        out.write("null");
    }
}
