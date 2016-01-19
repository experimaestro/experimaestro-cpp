package net.bpiwowar.xpm.manager.experiments;

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

import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.manager.QName;
import net.bpiwowar.xpm.scheduler.DatabaseObjects;
import net.bpiwowar.xpm.scheduler.Identifiable;
import net.bpiwowar.xpm.scheduler.Resource;
import net.bpiwowar.xpm.scheduler.Scheduler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A task contains resources and is linked to other dependent tasks
 */
public class TaskReference implements Identifiable {
    private long id;

    /**
     * The parents
     */
    private final Collection<TaskReference> parents = new ArrayList<>();

    /**
     * The children
     */
    private final Collection<TaskReference> children = new ArrayList<>();

    /**
     * The ID of the task
     */
    QName taskId;

    /**
     * The experiment ID
     */
    Experiment experiment;

    /**
     * The associated resources
     */
    Collection<Resource> resources = new ArrayList<>();

    public TaskReference() {
    }

    public TaskReference(QName taskId, Experiment experiment, ArrayList<TaskReference> parentTaskReferences) {
        this.taskId = taskId;
        this.experiment = experiment;
        parentTaskReferences.forEach(this::addParent);
    }

    public TaskReference(QName taskId, Experiment experiment) {
        this.taskId = taskId;
        this.experiment = experiment;
    }


    /**
     * Add a parent
     */
    public void addParent(TaskReference parent) {
        assert parent.id != -1;
        assert this.id != -1;

        parent.children.add(this);
        parents.add(parent);
    }

    /**
     * Associate a resource to this task reference
     *
     * @param resource The resource to add
     */
    public void add(Resource resource) {
        resources.add(resource);
    }

    /**
     * Save in database
     */
    public void save() throws SQLException {
        DatabaseObjects<TaskReference> references = Scheduler.get().taskReferences();
        references.save(this, "INSERT INTO ExperimentTasks(identifier, experiment) VALUES(?, ?, ?)", st -> {
            st.setString(1, taskId.toString());
            st.setLong(1, experiment.getId());
        });

    }

    public Collection<TaskReference> children() {
        return children;
    }

    public QName getTaskId() {
        return taskId;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public static TaskReference create(DatabaseObjects<TaskReference> db, ResultSet result) {
        try {
            String identifier = result.getString(1);
            long experimentId = result.getLong(2);
            Experiment experiment = Experiment.findById(experimentId);
            final TaskReference reference = new TaskReference(QName.parse(identifier), experiment);
            return reference;
        } catch (SQLException e) {
            throw new XPMRuntimeException(e, "Could not construct network share");
        }
    }
}
