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

package sf.net.experimaestro.manager;

import org.apache.commons.vfs2.FileSystemException;
import org.w3c.dom.Node;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.log.Logger;

/**
 * Represents an atomic value
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 13/9/12
 */
public class ValueType extends Type {
    final static private Logger LOGGER = Logger.getLogger();

    static final QName QNAME = null;

    private QName type;

    public ValueType(QName type) {
        super(null);
        this.type = type;
    }

    public QName getValueType() {
        return type;
    }

    @Override
    public String toString() {
        return super.toString() + ": " + type.toString();
    }

    @Override
    public boolean matches(String namespaceURI, String name) {
        return true;
    }

    @Override
    public void validate(Node node) {
        String x = Manager.unwrap(node);
        // Test if the value is OK
        try {

            switch (type.getNamespaceURI()) {
                case Manager.XMLSCHEMA_NS:
                    switch (type.getLocalPart()) {
                        case "string":
                            break; // we accepts anything
                        case "float":
                            Float.parseFloat(x);
                            break;
                        case "integer":
                            Integer.parseInt(x);
                            break;
                        default:
                            throw new ExperimaestroRuntimeException("Un-handled type [%s]");
                    }
                    break;

                case Manager.EXPERIMAESTRO_NS:
                    switch (type.getLocalPart()) {
                        // TODO: do those checks
                        case "directory":
                            LOGGER.info("Did not check if [%s] was a directory", x);
                            break;
                        case "file":
                            LOGGER.info("Did not check if [%s] was a file", x);
                            break;
                        default:
                            throw new ExperimaestroRuntimeException("Un-handled type [%s]");
                    }
                    break;

                default:
                    throw new ExperimaestroRuntimeException("Un-handled type [%s]");
            }

        } catch (NumberFormatException e) {
            ExperimaestroRuntimeException e2 = new ExperimaestroRuntimeException("Wrong value for type [%s]: %s", type, x);
            throw e2;
        }
    }

    public Object unwrap(Node node) {
        String x = Manager.unwrap(node);

        switch (type.getNamespaceURI()) {
            case Manager.XMLSCHEMA_NS:
                switch (type.getLocalPart()) {
                    case "string":
                        return x;
                    case "float":
                        return Float.parseFloat(x);
                    case "integer":
                        return Integer.parseInt(x);
                    default:
                        throw new ExperimaestroRuntimeException("Un-handled type [%s]");
                }

            case Manager.EXPERIMAESTRO_NS:
                switch (type.getLocalPart()) {
                    // TODO: do those checks
                    case "directory":
                    case "file":
                        try {
                            return Scheduler.getVFSManager().resolveFile(x);
                        } catch (FileSystemException e) {
                            throw new ExperimaestroRuntimeException(e);
                        }
                    default:
                        throw new ExperimaestroRuntimeException("Un-handled type [%s]");
                }

            default:
                throw new ExperimaestroRuntimeException("Un-handled type [%s]");
        }
    }
}
