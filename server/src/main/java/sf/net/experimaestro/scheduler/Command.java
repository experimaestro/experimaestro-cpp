/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.scheduler;

import com.sleepycat.persist.model.Persistent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import sf.net.experimaestro.utils.log.Logger;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 18/10/12
 */
@Persistent
public class Command implements CommandComponent {
    public final static Logger LOGGER = Logger.getLogger();
    ArrayList<CommandComponent> list;

    public Command() {
        list = new ArrayList<>();
    }

    public Command(Collection<? extends CommandComponent> c) {
        list = new ArrayList<>(c);
    }

    /**
     * Iterates over all the command components
     *
     * @return An iterable
     */
    @Override
    public Stream<? extends CommandComponent> allComponents() {
        return list.parallelStream().flatMap(CommandComponent::allComponents);
    }

    @Override
    public java.lang.String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (CommandComponent argument : list) {
            sb.append('\'');
            sb.append(argument.toString());
            sb.append('\'');
            if (first)
                first = false;
            else
                sb.append(',');
        }
        sb.append("]");
        return sb.toString();
    }


    public int size() {
        return list.size();
    }

    public void add(CommandComponent... arguments) {
        list.addAll(Arrays.asList(arguments));
    }

    public void add(java.lang.String... arguments) {
        Arrays.asList(arguments).forEach(t -> add(new String(t)));
    }


    public java.lang.String prepare(CommandEnvironment environment) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (CommandComponent component : list)
            sb.append(component.prepare(environment));
        return sb.toString();
    }

    public ArrayList<CommandComponent> list() {
        return this.list;
    }

    @Persistent
    public static class CommandOutput implements CommandComponent {
        /**
         * The output
         */
        Command command;

        /**
         * The object (or null if standard output)
         */
        String path;

        public CommandOutput(Command command, String path) {
            this.command = command;
            this.path = path;
        }

        @Override
        public java.lang.String prepare(CommandEnvironment environment) throws FileSystemException {
            throw new NotImplementedException();
        }
    }

    @Persistent
    public static class String implements CommandComponent {
        java.lang.String string;

        private String() {
        }

        public String(java.lang.String string) {
            this.string = string;
        }

        @Override
        public java.lang.String prepare(CommandEnvironment environment) {
            return string;
        }

        @Override
        public java.lang.String toString() {
            return string;
        }
    }

    @Persistent
    public static class Path implements CommandComponent {

        private java.lang.String filename;

        private Path() {
        }

        public Path(FileObject file) {
            filename = file.getName().getPath();
        }

        public Path(java.lang.String filename) {
            this.filename = filename;
        }

        @Override
        public java.lang.String prepare(CommandEnvironment environment) throws FileSystemException {
            FileObject object = Scheduler.getVFSManager().resolveFile(filename);
            return environment.resolve(object);
        }

        @Override
        public java.lang.String toString() {
            return java.lang.String.format("<xp:path>%s</xp:path>", filename);
        }
    }

    @Persistent
    public static class ParameterFile implements CommandComponent {
        java.lang.String key;
        byte[] content;

        private ParameterFile() {
        }

        public ParameterFile(java.lang.String key, byte[] content) {
            this.key = key;
            this.content = content;
        }

        @Override
        public java.lang.String prepare(CommandEnvironment environment) throws IOException {
            FileObject file = environment.getAuxiliaryFile(key, ".input");
            OutputStream out = file.getContent().getOutputStream();
            out.write(content);
            out.close();

            return environment.resolve(file);
        }

        @Override
        public java.lang.String toString() {
            return java.lang.String.format("ParameterFile(%s)", key);
        }
    }

    @Persistent
    static public class Pipe implements CommandComponent {
        static private Pipe PIPE = new Pipe();

        private Pipe() {}

        public static Pipe getInstance() {
            return PIPE;
        }

        @Override
        public java.lang.String prepare(CommandEnvironment environment) throws IOException {
            return null;
        }
    }

}
