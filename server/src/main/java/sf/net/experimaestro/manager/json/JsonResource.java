package sf.net.experimaestro.manager.json;

import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.ValueType;
import sf.net.experimaestro.scheduler.Resource;

import java.io.IOException;
import java.io.Writer;

import static java.lang.String.format;

/**
 * Json wrapper over resources
 */
public class JsonResource implements Json {
    Resource resource;

    public JsonResource(Resource resource) {
        this.resource = resource;
    }

    @Override
    public JsonResource clone() {
        return new JsonResource(resource);
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public Resource get() {
        return resource;
    }

    @Override
    public QName type() {
        return ValueType.XP_RESOURCE;
    }

    @Override
    public boolean canIgnore(JsonWriterOptions options) {
        return options.ignore.contains(ValueType.XP_RESOURCE);
    }

    @Override
    public void writeDescriptorString(Writer writer, JsonWriterOptions options) throws IOException {
        if (options.ignore.contains(ValueType.XP_RESOURCE)) {
            writer.write("null");
        } else {
            write(writer);
        }
    }

    @Override
    public void writeDescriptorString(Writer writer) throws IOException {

    }

    @Override
    public void write(Writer out) throws IOException {
        out.write(format("{ \"id\": \"%s\", \"$type\": \"%s\" }",
                resource.getPath().toString(),
                ValueType.XP_RESOURCE.toString()
        ));
    }

}
