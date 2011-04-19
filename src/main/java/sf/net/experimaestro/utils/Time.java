package sf.net.experimaestro.utils;

public class Time {
	/**
	 * Format a time given in seconds in the format xd yh zm ts where x,y,z and
	 * t are the number of days, hours, minutes and seconds respectively.
	 * 
	 * @param seconds
	 *            The number of seconds
	 */
	public static String formatTimeInSeconds(int seconds) {
		int minutes = seconds / 60;
		seconds %= 60;
		if (minutes > 0) {
			int hours = minutes / 60;
			minutes %= 60;
			if (hours > 0) {
				int days = hours / 24;
				hours %= 24;
				if (days > 0)
					return String.format("%dd %dh %dm %ds", days, hours,
							minutes, seconds);

				return String.format("%dh %dm %ds", hours, minutes, seconds);
			}

			return String.format("%dm %ds", minutes, seconds);
		}

		return String.format("%ds", seconds);
	}

	/**
	 * Format a time given in milliseconds in the format xd yh zm ts xms where
	 * x,y,z and t are the number of days, hours, minutes, seconds and
	 * milliseconds respectively.
	 * 
	 * @param seconds
	 *            The number of seconds
	 */
	public static String formatTimeInMilliseconds(long ms) {
		int seconds = (int) ms / 1000;
		ms %= 1000;
		if (seconds > 0)
			return String.format("%s %dms", formatTimeInSeconds(seconds), ms);

		return String.format("%dms", ms);
	}
}
