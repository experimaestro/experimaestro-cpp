package net.bpiwowar.xpm.manager.scripting;

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

import net.bpiwowar.xpm.utils.PathUtils;
import net.bpiwowar.xpm.utils.log.Logger;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A JavaScript wrapper for {@linkplain Path}
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Exposed(value = "Path")
public class ScriptingPath extends WrapperObject<Path> {
    final static Logger LOGGER = Logger.getLogger();

    public ScriptingPath() {
        super(null);
    }

    @Expose
    public ScriptingPath(Path path) {
        super(path);
    }

    @Expose
    public ScriptingPath(String path) throws FileSystemException {
        super(Paths.get(path));
    }

    @Expose
    static public Path toPath(@Argument(name = "path") String path) throws URISyntaxException, IOException {
        return PathUtils.toPath(path);
    }

    @Override
    @Expose("toString")
    public String toString() {
        return object == null ? "[null]" : object.toString();
    }

    @Expose("toSource")
    public String toSource() {
        return String.format("new Path(%s)", object.toString());
    }

    @Expose("uri")
    public String uri() {
        return object.toUri().toString();
    }


    @Help(value = "Get the parent file object")
    @Expose("get_parent")
    public ScriptingPath getParent() throws FileSystemException {
        return get_ancestor(1);
    }

    @Expose("resolve")
    public ScriptingPath resolve(String path) throws FileSystemException {
        return new ScriptingPath(this.object.resolve(path));
    }

    @Help(value = "Get the n<sup>th</sup> ancestor of this file object")
    @Expose("get_ancestor")
    public ScriptingPath get_ancestor(@Argument(type = "Integer", name = "levels") int level) throws FileSystemException {
        if (level < 0)
            throw new IllegalArgumentException("Level is negative (" + level + ")");

        java.nio.file.Path ancestor = this.object.normalize();
        while (--level >= 0)
            ancestor = ancestor.getParent();

        return new ScriptingPath(ancestor);
    }

    @Expose("read_all")
    public String readAll() throws IOException {
        try(BufferedReader reader = Files.newBufferedReader(object)) {
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
            "Each name given corresponds to a new path component starting from this file object.")
    public Path path(@Argument(type = "String", name = "name") Object... args) throws FileSystemException {
        java.nio.file.Path current = object;
        for (int i = 0; i < args.length; i++) {
            current = path(current, i, args[i]);
        }

        return current;
    }

    private Path path(Path current, int i, Object arg) throws FileSystemException {
        if (arg == null)
            throw new IllegalArgumentException(String.format("Undefined element (index %d) in path", i));

        String name = arg.toString();
        current = current.resolve(name);
        return current;
    }

    private java.nio.file.Path path(java.nio.file.Path current, List array) throws FileSystemException {
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
        Files.createDirectories(object);
    }

    @Expose("exists")
    public boolean exists() throws FileSystemException {
        return Files.exists(object);
    }

    @Expose("get_size")
    public long get_size() throws IOException {
        return Files.size(object);
    }

    @Expose("add_extension")
    @Help("Adds an extension to the current filename")
    public Object add_extension(String extension) throws FileSystemException {
        return object.getParent().resolve(object.getFileName().getName(0) + extension);
    }

    @Expose
    @Help("Removes extension to the current filename")
    public Object remove_extension(String extension) throws FileSystemException {
        String baseName = object.getFileName().getName(0).toString();
        if (baseName.endsWith(extension))
            baseName = baseName.substring(0, baseName.length() - extension.length());
        return object.getParent().resolve(baseName);
    }

    @Expose
    @Help("Find all the matching files within this folder")
    public List<Path> find_matching_files(@Argument(name = "regexp", type = "String", help = "The regular expression") String regexp) throws IOException {
        final Pattern pattern = Pattern.compile(regexp);
        final ArrayList<Path> array = new ArrayList<>();
        DirectoryStream<java.nio.file.Path> paths = Files.newDirectoryStream(object, f -> pattern.matcher(f.getFileName().toString()).matches());

        Iterator<java.nio.file.Path> iterator = paths.iterator();
        while (iterator.hasNext()) {
            array.add(iterator.next());
        }
        return array;
    }

    @Expose
    public void copy_to(@Argument(name = "destination") ScriptingPath destination) throws IOException {
        Files.copy(object, destination.object);
    }

    @Expose
    public PrintWriter output_stream() throws IOException {
        final OutputStream output = Files.newOutputStream(object);
        return new MyPrintWriter(Context.get(), output);
    }

    @Expose
    public BufferedReader input_stream() throws IOException {
        return new BufferedReader(new MyInputStreamReader(Context.get(), Files.newInputStream(object)));
    }

    public java.nio.file.Path getObject() {
        return object;
    }

    @Expose("get_path")
    @Help("Get the file path, ignoring the file scheme")
    public String get_path() {
        return object.toUri().getPath();
    }

    static class MyPrintWriter extends PrintWriter {
        private final Context context;

        public MyPrintWriter(Context context, OutputStream out) {
            super(out);
            this.context = context;
            context.register(this);
        }

        @Override
        public void close() {
            super.close();
            context.unregister(this);
        }
    }

    static class MyInputStreamReader extends InputStreamReader {
        final Context context;

        public MyInputStreamReader(Context context, InputStream in) {
            super(in);
            this.context = context;
            context.register(this);
        }

        @Override
        public void close() throws IOException {
            super.close();
            context.unregister(this);
        }
    }


}
