package net.bpiwowar.xpm.scheduler;

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

import net.bpiwowar.xpm.exceptions.LockException;
import net.bpiwowar.xpm.locks.FileLock;
import net.bpiwowar.xpm.locks.Lock;
import net.bpiwowar.xpm.utils.log.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A dependency type where the resource can accessed by just one
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@TypeIdentifier("EXCLUSIVE")
public class ExclusiveDependency extends Dependency {
    static final private Logger LOGGER = Logger.getLogger();

    public ExclusiveDependency(long fromId, long toId, Lock lock, DependencyStatus status) {
        super(fromId, toId, lock, status);
    }

    public ExclusiveDependency(Resource from, Resource to) {
        super(from);
    }

    @Override
    synchronized protected DependencyStatus _accept() {
        Resource from = getFrom();
        final Path file;
        file = from.getFileWithExtension(Resource.LOCK_EXTENSION);
        return Files.exists(file) ? DependencyStatus.WAIT : DependencyStatus.OK_LOCK;

    }

    @Override
    synchronized protected Lock _lock(String pid) throws LockException {
        Resource from = getFrom();
        Path file = from.getFileWithExtension(Resource.LOCK_EXTENSION);
        return new FileLock(file, true);
    }
}
