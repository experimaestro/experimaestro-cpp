package bpiwowar.expmanager.utils;

import java.io.File;

/**
 * Look for file changes
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class WatchFileMonitor {
	static public enum Mode {
		MODIFIED, DELETED
	}

	private File file;
	Mode mode;
	private final long pollingInterval;

	public WatchFileMonitor(File file, Mode mode, long pollingInterval) {
		this.file = file;
		this.mode = mode;
		this.pollingInterval = pollingInterval;

	}

	public WatchFileMonitor(File file, Mode mode) {
		this(file, mode, 5000);
	}

	public void take() {
		while (!good()) {
			synchronized (this) {
				try {
					wait(pollingInterval);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	private boolean good() {
		switch (mode) {
		case DELETED:
			return !file.exists();
		case MODIFIED:
			throw new RuntimeException("Not implemented");
		}
		return false;
	}
}
