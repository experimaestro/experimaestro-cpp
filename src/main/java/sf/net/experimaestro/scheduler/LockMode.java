package sf.net.experimaestro.scheduler;

import bpiwowar.argparser.EnumValue;

/**
 * Locking mode
 */
public enum LockMode {
	@EnumValue(value = "read-only", help = "Creates a resource that can only be read")
	/**
	 * The resource need not (and can not) be locked
	 */
	READ_ONLY,

	/**
	 * The resouce is locked globally (e.g. a generated file)
	 */
	@EnumValue(value = "exclusive-writer", help = "Creates a resource that can be read by many, but need exclusive write access")
	EXCLUSIVE_WRITER,

	/**
	 * A data resource that can be read by many, but written by at most one
	 * (e.g. Berkeley database with concurrent read but just one write access)
	 */
	@EnumValue(value = "single-writer", help = "Creates a resource that can be read by many, and can be written by at most one")
	SINGLE_WRITER,

	/**
	 * A resouce that can be read/write by many (typically a server). In this
	 * mode, there will be no lock tacken
	 */
	@EnumValue(value = "multiple-writer", help = "Creates a resource that can be read and written by many")
	MULTIPLE_WRITER;

}