package bpiwowar.io;
/**
 * 
 */


import java.io.PrintWriter;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * @author bpiwowar
 * @date Jan 11, 2008
 *
 */
public class LoggerPrintWriter extends PrintWriter  {
	/**
	 * Creates a PrintStream for a given logger at a given output level
	 */
	public LoggerPrintWriter(Logger logger, Level level) {
		super(new LoggerOutputStream(logger, level));
	}
}
