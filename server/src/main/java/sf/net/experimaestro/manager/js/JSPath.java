package sf.net.experimaestro.manager.js;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
 *
 * experimaestro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * experimaestro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Wrapper;
import sf.net.experimaestro.manager.scripting.Help;
import sf.net.experimaestro.manager.scripting.Argument;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonArray;
import sf.net.experimaestro.manager.json.JsonPath;
import sf.net.experimaestro.manager.scripting.Expose;
import sf.net.experimaestro.utils.log.Logger;

import java.io.*;
import java.nio.CharBuffer;
import java.nio.file.*;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A JavaScript wrapper for Path
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class JSPath extends JSBaseObject implements Wrapper {
    public static final String JSCLASSNAME = "Path";
    final static Logger LOGGER = Logger.getLogger();
    private Path path;

    public JSPath() {
    }

    @Expose
    public JSPath(Path path) {
        this.path = path;
    }

    @Expose
    public JSPath(String path) throws FileSystemException {
        this.path = Paths.get(path);
    }

    @Override
    @Expose("toString")
    public String toString() {
        return path == null ? "[null]" : path.toString();
    }

    @Expose("toSource")
    public String toSource() {
        return String.format("new Path(%s)", path.toString());
    }


    @Help(value = "Get the parent file object")
    @Expose("get_parent")
    public JSPath getParent() throws FileSystemException {
        return get_ancestor(1);
    }

    @Expose("resolve")
    public JSPath resolve(String path) throws FileSystemException {
        return new JSPath(this.path.resolve(path));
    }

    @Help(value = "Get the n<sup>th</sup> ancestor of this file object",
            arguments = @JSArguments(@Argument(type = "Integer", name = "levels")))
    @Expose("get_ancestor")
    public JSPath get_ancestor(int level) throws FileSystemException {
        if (level < 0)
            throw new IllegalArgumentException("Level is negative (" + level + ")");

        Path ancestor = this.path.normalize();
        while (--level >= 0)
            ancestor = ancestor.getParent();

        return new JSPath(ancestor);
    }

    @Expose("read_all")
    public String readAll() throws IOException {
        try(BufferedReader reader = Files.newBufferedReader(path)) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        }
    }


    @Expose("path")
    @Help(value = "Returns a file object corresponding to the path given in the arguments. " +
            "Each name given corresponds to a new path component starting from this file object.",
            arguments = @JSArguments({@Argument(type = "String", name = "name"), @Argument(name = "...")}))
    public JSPath path(Object... args) throws FileSystemException {
        Path current = path;
        for (int i = 0; i < args.length; i++) {
            current = path(current, i, args[i]);
        }

        return new JSPath(current);
    }

    private Path path(Path current, int i, Object arg) throws FileSystemException {
        if (arg == null)
            throw new IllegalArgumentException(String.format("Undefined element (index %d) in path", i));

        if (arg instanceof NativeArray)
            return path(current, (NativeArray) arg);

        if (arg instanceof JSJson) {
            Json json = ((JSJson) arg).getJson();
            if (json instanceof JsonArray)
                return path(current, (JsonArray) json);
        }

        String name = Context.toString(arg);
        current = current.resolve(name);
        return current;
    }

    private Path path(Path current, List array) throws FileSystemException {
        for (int i = 0; i < array.size(); i++) {
            Object value = array.get(i);
            current = path(current, i, value);
        }
        return current;
    }


    @Expose("mkdirs")
    @Help("Creates this folder, if it does not exist.  Also creates any ancestor\n" +
            "folders which do not exist.  This method does nothing if the folder\n" +
            "already exists.")
    public void mkdirs() throws IOException {
        Files.createDirectories(path);
    }

    @Expose("exists")
    public boolean exists() throws FileSystemException {
        return Files.exists(path);
    }

    @Expose("get_size")
    public long get_size() throws IOException {
        return Files.size(path);
    }

    @Expose("add_extension")
    @Help("Adds an extension to the current filename")
    public Object add_extension(String extension) throws FileSystemException {
        return path.getParent().resolve(path.getFileName().getName(0) + extension);
    }

    @Expose
    @Help("Removes extension to the current filename")
    public Object remove_extension(String extension) throws FileSystemException {
        String baseName = path.getFileName().getName(0).toString();
        if (baseName.endsWith(extension))
            baseName = baseName.substring(0, baseName.length() - extension.length());
        return path.getParent().resolve(baseName);
    }

    @Expose
    @Help("Find all the matching files within this folder")
    public JSJson find_matching_files(@Argument(name = "regexp", type = "String", help = "The regular expression") String regexp) throws IOException {
        final Pattern pattern = Pattern.compile(regexp);
        final JsonArray array = new JsonArray();
        DirectoryStream<Path> paths = Files.newDirectoryStream(path, f -> pattern.matcher(f.getFileName().toString()).matches());

        Iterator<Path> iterator = paths.iterator();
        while (iterator.hasNext()) {
            array.add(new JsonPath(iterator.next()));
        }
        return new JSJson(array);
    }

    @Expose
    public void copy_to(@Argument(name = "destination") JSPath destination) throws IOException {
        Files.copy(path, destination.path);
    }

    @Override
    public Object unwrap() {
        return path;
    }

    @Expose
    public PrintWriter output_stream() throws IOException {
        final OutputStream output = Files.newOutputStream(path);
        final PrintWriter writer = new MyPrintWriter(xpm(), output);
        return writer;
    }

    @Expose
    public BufferedReader input_stream() throws IOException {
        final BufferedReader reader = new BufferedReader(new MyInputStreamReader(xpm(), Files.newInputStream(path)));
        return reader;
    }

    public Path getPath() {
        return path;
    }

    @Expose("get_path")
    @Help("Get the file path, ignoring the file scheme")
    public String get_path() {
        return path.toUri().getPath();
    }

    static class MyPrintWriter extends PrintWriter {
        final XPMObject xpm;

        public MyPrintWriter(XPMObject xpm, OutputStream out) {
            super(out);
            this.xpm = xpm;
            xpm.register(this);
        }

        @Override
        public void close() {
            super.close();
            xpm.unregister(this);
        }
    }

    static class MyInputStreamReader extends InputStreamReader {
        final XPMObject xpm;

        public MyInputStreamReader(XPMObject xpm, InputStream in) {
            super(in);
            this.xpm = xpm;
            xpm.register(this);
        }

        @Override
        public void close() throws IOException {
            super.close();
            xpm.unregister(this);
        }
    }


}
