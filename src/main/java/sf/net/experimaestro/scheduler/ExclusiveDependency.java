/*
 * This file is part of experimaestro.
 * Copyright (c) 2013 B. Piwowarski <benjamin@bpiwowar.net>
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

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import sf.net.experimaestro.exceptions.LockException;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.utils.log.Logger;

/**
 * A dependency type where the resource can accessed by just one
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 29/1/13
 */
public class ExclusiveDependency extends Dependency {
    static final private Logger LOGGER = Logger.getLogger();

    protected ExclusiveDependency() {

    }

    public ExclusiveDependency(long from) {
        super(from);
    }


    @Override
    synchronized protected DependencyStatus _accept(Scheduler scheduler, Resource from) {
        Resource resource = getFrom(scheduler, from);
        resource.init(scheduler);
        final FileObject file;
        try {
            file = resource.getFileWithExtension(Resource.LOCK_EXTENSION);
            return file.exists() ? DependencyStatus.WAIT : DependencyStatus.OK_LOCK;
        } catch (FileSystemException e) {
            LOGGER.error(e, "Error while checking the presence of lock file for [%s]", from);
            return DependencyStatus.ERROR;

        }

    }

    @Override
    synchronized protected Lock _lock(Scheduler scheduler, Resource from, String pid) throws LockException {
        Resource resource = getFrom(scheduler, from);
        try {
            FileObject file = resource.getFileWithExtension(Resource.LOCK_EXTENSION);
            final Lock lockFile = resource.getMainConnector().createLockFile(file.getName().getPath());
            return lockFile;
        } catch (FileSystemException e) {
            throw new LockException(e);
        }
    }
}
