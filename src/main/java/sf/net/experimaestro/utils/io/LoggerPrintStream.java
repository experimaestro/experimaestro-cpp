package sf.net.experimaestro.utils.io;
/**
 * 
 */


import java.io.PrintStream;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * @author bpiwowar
 * @date Jan 11, 2008
 *
 */
public class LoggerPrintStream extends PrintStream  {
	/**
	 * Creates a PrintStream for a given logger at a given output level
	 */
	public LoggerPrintStream(Logger logger, Level level) {
		super(new LoggerOutputStream(logger, level));
	}
}
