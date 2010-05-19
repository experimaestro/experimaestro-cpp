package bpiwowar.expmanager.rsrc;

import java.io.IOException;
import java.util.Arrays;

import bpiwowar.argparser.ListAdaptator;
import bpiwowar.log.Logger;
import bpiwowar.utils.Output;

/**
 * A command line task
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class CommandLineTask extends Task {
	final static private Logger LOGGER = Logger.getLogger();

	private String[] command;

	/**
	 * Constructs the command line
	 * @param taskManager
	 * @param identifier
	 * @param command
	 */
	public CommandLineTask(TaskManager taskManager, String identifier, String[] command) {
		super(taskManager, identifier);

		this.command = new String[] {
				"/bin/bash",
				"-c",
				String.format("( %s ) > %s.out 2> %2$s.err", Output.toString(
						" ", ListAdaptator.create(command),
						new Output.Formatter<String>() {
							public String format(String t) {
								StringBuilder sb = new StringBuilder();
								for (int i = 0; i < t.length(); i++) {
									final char c = t.charAt(i);
									switch (c) {
									case ' ':
										sb.append("\\ ");
										break;
									default:
										sb.append(c);
									}
								}
								return sb.toString();
							}
						}), identifier, identifier) };

	}

	@Override
	protected int doRun() throws IOException, InterruptedException {
		LOGGER.info("Evaluating command %s", Arrays.toString(command));
		Process p = Runtime.getRuntime().exec(command);
		synchronized (p) {
			LOGGER.info("Waiting for the process to end");
			int code = p.waitFor();
			if (code != 0)
				throw new RuntimeException("Process ended with errors (code "
						+ code + ")");
			LOGGER.info("Done");
			return code;
		}
	}
}
