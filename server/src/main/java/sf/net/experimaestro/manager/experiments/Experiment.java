package sf.net.experimaestro.manager.experiments;

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

import java.nio.file.Path;
import sf.net.experimaestro.annotations.Exposed;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.jpa.PathConverter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;

/**
 * An experiment
 */
@Entity
@Exposed
public class Experiment {
    /** Experiment taskId */
    @Id
    long id;

    /** Tasks */
    @ManyToMany(fetch = FetchType.LAZY)
    Collection<TaskReference> tasks = new ArrayList<>();

    /** Working directory */
    @Convert(converter = PathConverter.class)
    Path workingDirectory;

    /** Timestamp */
    private final long timestamp;

    /** Identifier */
    String identifier;

    /** Scheduler */
    transient private Scheduler scheduler;

    /**
     * New task
     * @param identifier The experiment taskId
     * @param workingDirectory The working directory for this experiment
     */
    public Experiment(String identifier, long timestamp, Path workingDirectory) {
        this.identifier = identifier;
        this.timestamp = timestamp;
        this.workingDirectory = workingDirectory;
    }

    public void init(Scheduler scheduler) {
        this.scheduler = scheduler;
    }
}
