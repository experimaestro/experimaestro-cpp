package bpiwowar.expmanager.utils;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PID {

	/**
	 * Get the PID of a process
	 * 
	 * @param process
	 * @return
	 */
	public static int getPID(java.lang.Process process) {
		Field field;
		try {
			// Won't work on Windows
			field = process.getClass().getDeclaredField(osType == OSType.WINDOWS ? "handle": "pid");
			field.setAccessible(true);
			int pid =  field.getInt(process);
			field.setAccessible(false);
			return pid;
		} catch (SecurityException e) {
			throw new RuntimeException("Could not get the PID of the process",
					e);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Could not get the PID of the process",
					e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Could not get the PID of the process",
					e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Could not get the PID of the process",
					e);
		}
	}

	enum OSType {
		WINDOWS, UNIX, MAC
	}

	static OSType osType;

	static {
		String os = System.getProperty("os.name").toLowerCase();
		if (os.indexOf("win") >= 0)
			osType = OSType.WINDOWS;
		else if (os.indexOf("mac") >= 0)
			osType = OSType.MAC;
		else if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0)
			osType = OSType.UNIX;

	}

	/**
	 * Get the PID of the java runtime
	 * 
	 * @return
	 */
	public static int getPID() {
		RuntimeMXBean rtb = ManagementFactory.getRuntimeMXBean();
		String processName = rtb.getName();
		Integer pid = tryPattern1(processName);
		return pid;
	}

	private static Integer tryPattern1(String processName) {
		Integer result = null;

		/* tested on: */
		/* - windows xp sp 2, java 1.5.0_13 */
		/* - mac os x 10.4.10, java 1.5.0 */
		/* - debian linux, java 1.5.0_13 */
		/* all return pid@host, e.g 2204@antonius */

		Pattern pattern = Pattern.compile("^([0-9]+)@.+$",
				Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(processName);
		if (matcher.matches()) {
			result = new Integer(Integer.parseInt(matcher.group(1)));
		}
		return result;

	}
}
