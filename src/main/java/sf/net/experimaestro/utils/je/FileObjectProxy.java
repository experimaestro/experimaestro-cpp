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

package sf.net.experimaestro.utils.je;

import com.sleepycat.persist.model.Persistent;
import com.sleepycat.persist.model.PersistentProxy;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.provider.local.LocalFile;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.scheduler.Scheduler;

@Persistent(proxyFor = FileObject.class)
public class FileObjectProxy implements PersistentProxy<FileObject> {

    @Persistent(proxyFor = LocalFile.class)
    public static class LocalProxy extends FileObjectProxy {
    }

	String uri;
	
	@Override
	public void initializeProxy(FileObject object) {
        uri = object.toString();
	}

	@Override
	public FileObject convertProxy() {
        try {
            return Scheduler.getVFSManager().resolveFile(uri);
        } catch (FileSystemException e) {
            throw new ExperimaestroRuntimeException(e);
        }
    }

}
