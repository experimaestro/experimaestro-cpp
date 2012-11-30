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
import sf.net.experimaestro.connectors.SingleHostConnector;

import java.util.ArrayList;

/**
 * A command argument that can be processed depending on where the command is running.
 *
 * This is used e.g. when there is a path that has to be transformed because the running host
 * has a different path mapping than the host where the command line was configured.
 *
 * It is the concatenation of
 * <ul>
 *     <li>strings</li>
 *     <li>paths</li>
 * </ul>
 *
 * Paths can be localized depending on where the command is run.
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 18/10/12
 */
@Persistent
public class CommandArgument {
    private ArrayList<Component> components = new ArrayList<>();



    static public interface Component {
        java.lang.String resolve(SingleHostConnector connector) throws FileSystemException;
    }

    @Persistent
    static public class String implements Component {
        java.lang.String string;

        private String() {}

        public String(java.lang.String string) {
            this.string = string;
        }

        @Override
        public java.lang.String resolve(SingleHostConnector connector) {
            return string;
        }

        @Override
        public java.lang.String toString() {
            return string;
        }
    }


    @Persistent
    static public class Path implements Component {

        private java.lang.String filename;

        private Path() {}

        public Path(FileObject file) {
            filename = file.getName().getPath();
        }

        public Path(java.lang.String filename) {
            this.filename = filename;
        }

        @Override
        public java.lang.String resolve(SingleHostConnector connector) throws FileSystemException {
            FileObject object = Scheduler.getVFSManager().resolveFile(filename);
            return connector.resolve(object);
        }

        @Override
        public java.lang.String toString() {
            return java.lang.String.format("<xp:path>%s</xp:path>", filename);
        }
    }


    public CommandArgument(java.lang.String string) {
        add(string);
    }

    public CommandArgument() {
    }

    public void add(java.lang.String string) {
        components.add(new String(string));
    }

    public void add(Component component) {
        components.add(component);
    }

    public java.lang.String resolve(final SingleHostConnector connector) throws FileSystemException {
        StringBuilder sb = new StringBuilder();
        for(Component component: components)
            sb.append(component.resolve(connector));
        return sb.toString();
    }

    @Override
    public java.lang.String toString() {
        StringBuilder sb = new StringBuilder();
        for(Component component: components)
            sb.append(component.toString());
        return sb.toString();
    }
}
