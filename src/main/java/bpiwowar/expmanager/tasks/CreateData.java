package bpiwowar.expmanager.tasks;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import bpiwowar.argparser.ArgParseException;
import bpiwowar.argparser.ArgParser;
import bpiwowar.argparser.ArgParserOption;
import bpiwowar.argparser.Argument;
import bpiwowar.argparser.ArgumentRegexp;
import bpiwowar.argparser.IllegalArgumentValue;
import bpiwowar.expmanager.rsrc.SimpleData.Mode;
import bpiwowar.utils.GenericHelper;

/**
 * Create a data resource
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class CreateData {

	@Argument(name = "basename", required = true, help = "Basename for the resource")
	File basename;

	@Argument(name = "mode")
	Mode mode;

	/**
	 * Parameters associated to this
	 */
	Map<String, String> parameters = GenericHelper.newTreeMap();

	@Argument(name = "param", required = true)
	@ArgumentRegexp("^([^=])=(.*)$")
	void addParameter(String name, String value) {
		parameters.put(name, value);
	}

	@Argument(name = "data", required = true, help = "Produced data")
	void addData(File file) {

	}

	public CreateData(String[] args) throws IllegalArgumentValue, IOException,
			ArgParseException {
		// Read the arguments
		ArgParser argParser = new ArgParser("create-job");
		argParser.addOptions(this);
		String[] command = argParser.matchAllArgs(args, 0,
				ArgParserOption.STOP_FIRST_UNMATCHED,
				ArgParserOption.EXCEPTION_ON_ERROR);

		// Now, command is the common to execute
	}
}
