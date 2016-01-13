package net.bpiwowar.xpm.manager.scripting;

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
import net.bpiwowar.xpm.manager.Repository;
import net.bpiwowar.xpm.scheduler.Scheduler;
import net.bpiwowar.xpm.utils.Cleaner;
import net.bpiwowar.xpm.utils.log.Logger;

import java.io.Closeable;

/**
 * Static context when running a script
 */
public class StaticContext implements AutoCloseable {
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

    /**
     * Main logger
     */
    final Logger mainLogger;

    /**
     * The resource cleaner
     * <p>
     * Used to close objects at the end of the execution of a script
     */
    Cleaner cleaner;


    public void register(Closeable closeable) {
        cleaner.register(closeable);
    }

    public void unregister(AutoCloseable autoCloseable) {
        cleaner.unregister(autoCloseable);
    }


    public StaticContext(Scheduler scheduler, LoggerRepository loggerRepository) {
        this.scheduler = scheduler;
        this.loggerRepository = loggerRepository;
        this.mainLogger = (Logger)loggerRepository.getLogger("xpm", Logger.factory());
        this.cleaner = new Cleaner();
    }

    public ScriptContext scriptContext() {
        return new ScriptContext(this);
    }

    public StaticContext repository(Repository repository) {
        this.repository = repository;
        return this;
    }

    public Logger getMainLogger() {
        return mainLogger;
    }

    @Override
    public void close() {
        cleaner.close();
    }
}
