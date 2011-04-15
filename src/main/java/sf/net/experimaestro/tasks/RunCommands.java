package sf.net.experimaestro.tasks;

import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;

@TaskDescription(name = "run-commands", project = { "Run commands given in an XML file" })
public class RunCommands extends AbstractTask {
	@Override
	public int execute() throws Throwable {
		return 0;
	}
}
