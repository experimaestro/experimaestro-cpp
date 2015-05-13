package sf.net.experimaestro.manager.plans;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
 *
 * experimaestro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * experimaestro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.apache.log4j.spi.LoggerRepository;
import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.Cleaner;
import sf.net.experimaestro.utils.log.Logger;

/**
 * Static context when running a script
 */
public class StaticContext {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * The scheduler
     */
    Scheduler scheduler;

    /**
     * The logger
     */
    LoggerRepository loggerRepository;

    /**
     * Task repository
     */
    Repository repository;


    public StaticContext(Scheduler scheduler, LoggerRepository loggerRepository) {
        this.scheduler = scheduler;
        this.loggerRepository = loggerRepository;
    }

    public StaticContext(Scheduler scheduler) {
        this(scheduler, LOGGER.getLoggerRepository());
    }

    public ScriptContext scriptContext() {
        return new ScriptContext(this);
    }

    public StaticContext repository(Repository repository) {
        this.repository = repository;
        return this;
    }
}
