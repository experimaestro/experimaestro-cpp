package sf.net.experimaestro.manager.json;

import com.sleepycat.persist.model.Persistent;
import org.apache.commons.vfs2.FileObject;
import org.json.simple.JSONValue;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.ValueType;

import java.io.IOException;
import java.io.Writer;

/**
 * The default interface for JsonFileObject
 */
@Persistent
public class JsonFileObject implements Json {
    FileObject fileObject;

    private JsonFileObject() {
    }

    public JsonFileObject(FileObject fileObject) {
        this.fileObject = fileObject;
    }

    @Override
    public Json clone() {
        return new JsonFileObject(fileObject);
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public FileObject get() {
        return fileObject;
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
            writer.write(JSONValue.escape(options.resolver.apply(fileObject)));
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
