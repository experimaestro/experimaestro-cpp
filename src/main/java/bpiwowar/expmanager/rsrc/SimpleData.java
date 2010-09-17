package bpiwowar.expmanager.rsrc;

import bpiwowar.log.Logger;

/**
 * 
 * A single piece of data that can be locked in various ways
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class SimpleData extends Data {
	final static private Logger LOGGER = Logger.getLogger();

	public SimpleData(TaskManager taskManager, String identifier,
			LockMode mode, boolean generated) {
		super(taskManager, identifier, mode);
		LOGGER.info("New resource: simple data (%s) with mode %s (generated = %b)", identifier,
				mode, generated);
		this.generated = generated;
	}

}
