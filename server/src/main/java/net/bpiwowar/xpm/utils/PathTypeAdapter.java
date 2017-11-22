package net.bpiwowar.xpm.utils;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathTypeAdapter extends TypeAdapter<Path> {
    @Override
    public void write(JsonWriter out, Path value) throws IOException {
        out.value(value.toAbsolutePath().toString());
    }

    @Override
    public Path read(JsonReader in) throws IOException {
        return Paths.get(in.nextString());
    }
}
