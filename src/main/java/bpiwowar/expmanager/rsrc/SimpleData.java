package bpiwowar.expmanager.rsrc;

import bpiwowar.argparser.EnumValue;

/**
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class SimpleData extends Data {
	/**
	 * The access mode
	 */
	private final Mode mode;

	/**
	 * Status
	 */

	public enum Mode {
		@EnumValue(value = "read-only", help = "Creates a resource that can only be read")
		READ_ONLY,

		@EnumValue(value = "exclusive-write", help = "Creates a resource that can be read by many, but need exclusive write access")
		EXCLUSIVE_WRITE,

		@EnumValue(value = "single-writer", help = "Creates a resource that can be read by many, and can be written by at most one")
		SINGLE_WRITER,

		@EnumValue(value = "multiple-writers", help = "Creates a resource that can be read by many, but can write access")
		MULTIPLE_WRITERS,
	}

	public SimpleData(TaskManager taskManager, String identifier, Mode mode) {
		super(taskManager, identifier);
		this.mode = mode;
	}
	
	int writers, readers;
	
	@Override
	public boolean isReady(DependencyType type) {
		switch (type) {
		case GENERATED:
			return isReady();
		
		case READ_ACCESS:
			return isReady();
			
		case WRITE_ACCESS:
			return isReady();
		}
		return false;
	}

}
