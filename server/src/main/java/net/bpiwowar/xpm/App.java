package net.bpiwowar.xpm;

import net.bpiwowar.xpm.tasks.ServerCommand;
import picocli.CommandLine;

import java.util.concurrent.Callable;

/**
 * Run an experiment
 *
 * @author Benjamin Piwowarski
 */
@CommandLine.Command(subcommands = ServerCommand.class)
public class App implements Callable<Void> {
    static public void main(String [] args) {
        CommandLine.call(new App(), System.err, args);
    }

    @Override
    public Void call() throws Exception {
        return null;
    }
}
