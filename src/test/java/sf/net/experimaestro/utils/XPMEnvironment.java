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

package sf.net.experimaestro.utils;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import sf.net.experimaestro.scheduler.Scheduler;

import java.io.File;
import java.io.IOException;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 30/1/13
 */
public class XPMEnvironment {
    protected TemporaryDirectory directory;
    public Scheduler scheduler;

    private int count = 0;

    @BeforeClass
    public void init() throws IOException {
        if (count++ == 0) {
            directory = new TemporaryDirectory("scheduler-tests", "dir");
            final File dbFile = new File(directory.getFile(), "db");
            dbFile.mkdir();
            scheduler = new Scheduler(dbFile);
        }
    }

    @AfterClass
    public void close() {
        if (--count == 0)
            scheduler.close();
    }


}
