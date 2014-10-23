package sf.net.experimaestro.scheduler;

import org.testng.annotations.Test;
import sf.net.experimaestro.exceptions.ExperimaestroCannotOverwrite;
import sf.net.experimaestro.utils.ThreadCount;
import sf.net.experimaestro.utils.XPMEnvironment;

import java.io.File;
import java.io.IOException;

/**
 * Test the persistency and consistency of the information after the server restarts
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 20/12/13
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
