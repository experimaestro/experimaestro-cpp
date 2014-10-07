package net.bpiwowar.experimaestro.tasks;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * An adapter for files
 */
public class FileAdapter extends ReaderTypeAdapter<File> {

    public static final String FILE_PROTOCOL = "file://";

    @Override
    public File read(JsonReader in) throws IOException {
        final String s = in.nextString();

        if (s.startsWith("file://")) {
            return new File(s.substring(FILE_PROTOCOL.length()));
        }

        return new File(s);
    }
}
