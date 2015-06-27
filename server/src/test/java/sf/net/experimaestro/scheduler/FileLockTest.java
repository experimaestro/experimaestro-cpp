package sf.net.experimaestro.scheduler;

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

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import sf.net.experimaestro.locks.FileLock;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.utils.XPMEnvironment;

import java.io.File;

/**
 * Test for command line task
 */
public class FileLockTest extends XPMEnvironment {

    @BeforeSuite
    public static void setup() throws Throwable {
        prepare();
    }

    @Test(enabled = true)
    public void testSave() throws Exception {
        final File testDir = mkTestDir();

        final FileLock lock = new FileLock(testDir.toPath().resolve("lock"), true);
        lock.save();

        Scheduler.get().locks().forget(lock.getId());
        final FileLock otherLock = (FileLock)Lock.findById(lock.getId());

        assert otherLock.path().equals(lock.path());
    }

}
