package net.bpiwowar.xpm.test;

import net.bpiwowar.xpm.manager.tasks.AbstractTask;
import net.bpiwowar.xpm.manager.tasks.JsonArgument;
import net.bpiwowar.xpm.manager.tasks.TaskDescription;

import java.util.Random;

/**
 *
 */
@TaskDescription(id = "test:simple", output = "test:simple",
        description = "Outputs a tab-separated value stream with" +
                "<pre>TERM TID DID FIELD DOC_LENGHT POSITION_1 POSITION_2 ...</pre>",
        registry = Registry.class)
public class Simple extends AbstractTask {
    @JsonArgument(help = "Maximum (approximate through sampling) number of documents to output for each sampled term")
    long maxdocuments = Long.MAX_VALUE;

    @JsonArgument(optional = true)
    long seed = new Random().nextLong();

    @Override
    public void execute() throws Throwable {
        progress(.2);
    }
}
