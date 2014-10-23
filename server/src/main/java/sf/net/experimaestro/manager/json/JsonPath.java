package sf.net.experimaestro.manager.json;

import java.nio.file.Path;
import org.json.simple.JSONValue;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.ValueType;

import java.io.IOException;
import java.io.Writer;

/**
 * The default interface for JsonPath
 */
public class JsonPath implements Json {
    Path path;

    private JsonPath() {
    }

    public JsonPath(Path path) {
        this.path = path;
    }

    @Override
    public Json clone() {
        return new JsonPath(path);
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public Path get() {
        return path;
    }

    @Override
    public QName type() {
        return ValueType.XP_FILE;
    }

    @Override
    public boolean canIgnore(JsonWriterOptions options) {
        return options.ignore.contains(ValueType.XP_FILE);
    }

    @Override
    public void writeDescriptorString(Writer writer, JsonWriterOptions options) throws IOException {
        if (options.ignore.contains(ValueType.XP_FILE)) {
            writer.write("null");
        } else {
            writer.write('"');
            writer.write(JSONValue.escape(options.resolver.apply(path)));
            writer.write('"');
        }
    }

    @Override
    public void write(Writer out) throws IOException {
        out.write('"');
        out.write(JSONValue.escape(get().toString()));
        out.write('"');
    }
}
