package sf.net.experimaestro.scheduler;

import static java.lang.String.format;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.utils.Output;
import sf.net.experimaestro.utils.log.Logger;
import bpiwowar.argparser.ListAdaptator;

import com.sleepycat.persist.model.Persistent;

/**
 * A command line task (executed with the default shell)
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Persistent
public class CommandLineTask extends Job {
	final static private Logger LOGGER = Logger.getLogger();

	/**
	 * The command to execute
	 */
	private String[] command;

	/**
	 * The shell command used
	 */
	final String shellCommand = "/bin/bash";

	private String[] envp = null;

	private File workingDirectory;

	protected CommandLineTask() {
	}

	/**
	 * Constructs the command line
	 * 
	 * @param scheduler
	 * @param identifier
	 * @param command
	 * @throws FileNotFoundException
	 */
	public CommandLineTask(Scheduler scheduler, String identifier,
			String[] commandArgs, Map<String, String> env, File workingDirectory) {

		super(scheduler, identifier);

		LOGGER.info("Command is %s", Arrays.toString(commandArgs));

		// Copy the environment
		if (env != null) {
			envp = new String[env.size()];
			int i = 0;
			for (Map.Entry<String, String> entry : env.entrySet())
				envp[i++] = format("%s=%s", entry.getKey(), entry.getValue());
		}
		this.workingDirectory = workingDirectory;

		// Construct command
		this.command = new String[] {
				shellCommand,
				"-c",
				String.format("( %s ) > %s.out 2> %2$s.err", Output.toString(
						" ", ListAdaptator.create(commandArgs),
						new Output.Formatter<String>() {
							public String format(String t) {
								if (t.equals(""))
									return "\"\"";
								
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

	public CommandLineTask(Scheduler scheduler, String identifier,
			String[] command) {
		this(scheduler, identifier, command, null, null);
	}

	@Override
	protected int doRun(ArrayList<Lock> locks) throws IOException,
			InterruptedException {
		// Runs the command
		LOGGER.info("Evaluating command [%s] %s with environment %s",
				workingDirectory, Arrays.toString(command),
				Arrays.toString(envp));

		// Write command
		PrintWriter writer = new PrintWriter(new File(String.format("%s.run",
				identifier)));
		writer.format("%nCommand:%s%n", command[2]);
		writer.format("Working directory %s%n", workingDirectory);
		writer.format("Environment:%n%s%n%n", Arrays.toString(envp));

		writer.close();

		// --- Execute command

		Process p = null;
		try {
			p = Runtime.getRuntime().exec(command, envp, workingDirectory);

			// Changing the ownership of the different logs
			final int pid = sf.net.experimaestro.utils.PID.getPID(p);
			for (Lock lock : locks) {
				lock.changeOwnership(pid);
			}

			synchronized (p) {
				LOGGER.info("Waiting for the process (PID %d) to end", pid);
				int code = p.waitFor();
				if (code != 0)
					throw new RuntimeException(
							"Process ended with errors (code " + code + ")");
				LOGGER.info("Process (PID %d) ended without error", pid);
				return code;
			}
		} finally {
			if (p != null) {
				p.getInputStream().close();
				p.getOutputStream().close();
				p.getErrorStream().close();
			}
		}
	}
	
	@Override
	public void printHTML(PrintWriter out, PrintConfig config) {
		super.printHTML(out, config);
		out.format("<div><b>Command</b>: %s</div>", command[2]);
		out.format("<div><b>Working directory</b> %s</div>", workingDirectory);
		out.format("<div><b>Environment</b>: %s</div>", Arrays.toString(envp));
	}
}
