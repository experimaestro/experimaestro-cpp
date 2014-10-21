/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.manager;

import sf.net.experimaestro.scheduler.ResourceLocator;
import sf.net.experimaestro.utils.log.Logger;

import java.util.TreeMap;

/**
 * Manages a set of repositories, each associated to an identifier
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 15/6/12
 */
public class Repositories extends Repository {
    /**
     * Our logger
     */
    static final private Logger LOGGER = Logger.getLogger();

    /**
     * Our repositories
     */
    TreeMap<ResourceLocator, RepositoryInformation> repositories = new TreeMap<>();

    /**
     * Creates a new set of repositories
     */
    public Repositories(ResourceLocator locator) {
        super(locator);
    }

    /**
     * Returns a task factory
     *
     * @param name The qualified name of the factory
     * @return A TaskFactory object or null if not found
     */
    public TaskFactory getFactory(QName name) {
        TaskFactory factory = super.getFactory(name);
        if (factory != null)
            return factory;

        int priority = 0;
        for (RepositoryInformation repository : repositories.values()) {

            if (priority < repository.priority || factory == null) {
                final TaskFactory aFactory = repository.repository.getFactory(name);
                if (aFactory != null) {
                    factory = aFactory;
                    priority = repository.priority;
                }
            }
        }

        return factory;
    }

    /**
     * Adds a new repository with a given priority
     *
     * @param repository
     * @param priority
     */
    public void add(AbstractRepository repository, int priority) {
        LOGGER.info("Adding repository %s", repository.identifier);
        repositories.put(repository.identifier, new RepositoryInformation(repository, priority));
    }

    static private class RepositoryInformation {
        AbstractRepository repository;
        int priority;

        private RepositoryInformation(AbstractRepository repository, int priority) {
            this.repository = repository;
            this.priority = priority;
        }
    }
}
