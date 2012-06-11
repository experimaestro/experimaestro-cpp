/*
 *
 *  This file is part of experimaestro.
 *  Copyright (c) 2011 B. Piwowarski <benjamin@bpiwowar.net>
 *
 *  experimaestro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  experimaestro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package sf.net.experimaestro.locks;

import java.io.File;
import java.io.IOException;

import sf.net.experimaestro.utils.WatchFileMonitor;
import sf.net.experimaestro.utils.log.Logger;


/**
 * A simple file lock for the resource
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class FileLock implements Lock {
	final static private Logger LOGGER = Logger.getLogger();

	/**
	 * Lock
	 */
	File lockFile;

	/**
	 * Lock a file. If the file exists, waits for it to be deleted.
	 * 
	 * @param lockFile
	 * @throws IOException
	 */
	public FileLock(File lockFile, boolean wait) throws UnlockableException {
		this.lockFile = lockFile;
		WatchFileMonitor monitor = new WatchFileMonitor(lockFile,
				WatchFileMonitor.Mode.DELETED);
		try {
			while (!lockFile.createNewFile())
				if (wait)
					monitor.take();
				else throw new UnlockableException();
			lockFile.deleteOnExit();
		} catch (IOException e) {
			throw new UnlockableException("Could not create the lock file");
		}
	}

	public FileLock(File lockFile) throws UnlockableException {
		this(lockFile, true);
	}

    public FileLock(String lockFile) throws UnlockableException {
        this(new File(lockFile), true);
    }
	/*
	 * (non-Javadoc)
	 * 
	 * @see bpiwowar.expmanager.rsrc.Lock#dispose()
	 */
	public boolean dispose() {
		if (lockFile == null)
			return true;

		if (lockFile.exists()) {
			boolean success = lockFile.delete();
			lockFile = null;
			if (!success)
				LOGGER.warn("Could not delete lock file %s", lockFile);
			return success;
		}
		return true;
	}

	@Override
	protected void finalize() throws Throwable {
		dispose();
	}

	public void changeOwnership(int pid) {
		// TODO Auto-generated method stub

	}

}