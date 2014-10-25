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

import org.testng.annotations.Test;
import sf.net.experimaestro.exceptions.ExperimaestroCannotOverwrite;
import sf.net.experimaestro.utils.ThreadCount;
import sf.net.experimaestro.utils.XPMEnvironment;

import java.io.File;
import java.io.IOException;

/**
 * Test the persistency and consistency of the information after the server restarts
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class SchedulerRestartTest extends XPMEnvironment {
    @Test(description = "Run a job over restarts")
    public void test_running_job() throws
           IOException, InterruptedException, ExperimaestroCannotOverwrite {

        File jobDirectory = mkTestDir();
        ThreadCount counter = new ThreadCount();

        int one_hour = 60 * 60 * 1000;
        WaitingJob job = new WaitingJob(counter, jobDirectory, "job", new WaitingJob.Action(one_hour, 0, 0));
        scheduler.store(job, false);


    }


}
