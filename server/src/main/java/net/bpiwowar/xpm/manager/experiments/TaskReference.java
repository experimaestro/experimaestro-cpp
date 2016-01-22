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

import net.bpiwowar.xpm.exceptions.CloseException;
import net.bpiwowar.xpm.exceptions.WrappedException;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.manager.TypeName;
import net.bpiwowar.xpm.scheduler.DatabaseObjects;
import net.bpiwowar.xpm.scheduler.Identifiable;
import net.bpiwowar.xpm.scheduler.Resource;
import net.bpiwowar.xpm.scheduler.Scheduler;
import net.bpiwowar.xpm.utils.CloseableIterable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A task contains resources and is linked to other dependent tasks
 */
public class TaskReference implements Identifiable {
    static private final String SELECT_BEGIN = "SELECT id, identifier, experiment FROM ExperimentTasks";

    private long id;

    /**
     * The parents
     */
    private ArrayList<TaskReference> parents;

    /**
     * The children
     */
    private ArrayList<TaskReference> children;

    /**
     * The ID of the task
     */
    TypeName taskId;

    /**
     * The experiment ID
     */
    Experiment experiment;

    /**
     * The associated resources
     */
    List<Resource> resources = new ArrayList<>();

    public TaskReference() {
    }

    public TaskReference(TypeName taskId, Experiment experiment, ArrayList<TaskReference> parentTaskReferences) {
        this.taskId = taskId;
        this.experiment = experiment;
        if (parentTaskReferences != null) {
            parents = new ArrayList<>();
            parentTaskReferences.forEach(parents::add);
            resources = new ArrayList<>();
            children = new ArrayList<>();
        }
    }

    /**
     * Construct from DB
     */
    public TaskReference(TypeName taskId, Experiment experiment) {
        this(taskId, experiment, null);
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
    public void add(Resource resource) throws SQLException {
        try (PreparedStatement st = Scheduler.get().prepareStatement("INSERT INTO ExperimentResources(task, resource) VALUES (?,?)")) {
            st.setLong(1, getId());
            st.setLong(2, resource.getId());
            st.execute();
        }

        resources.add(resource);
    }

    /**
     * Save in database
     */
    public void save() throws SQLException {
        final Scheduler scheduler = Scheduler.get();
        DatabaseObjects<TaskReference> references = scheduler.taskReferences();
        references.save(this, "INSERT INTO ExperimentTasks(identifier, experiment) VALUES(?, ?)", st -> {
            st.setString(1, taskId.toString());
            st.setLong(2, experiment.getId());
        });

        // Now, add to experiment object
        experiment.add(this);

        // Insert into hierarchy
        try (PreparedStatement st = scheduler.prepareStatement("INSERT INTO ExperimentHierarchy(parent,child) VALUES (?, ?)")) {
            st.setLong(2, getId());
            for (TaskReference parent : parents) {
                st.setLong(1, parent.getId());
                st.execute();
            }

        }

        // Now, add as children of parents
        parents.forEach(p -> p.getChildren().add(this));

    }

    public Collection<TaskReference> children() {
        return children;
    }

    public TypeName getTaskId() {
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
            long id = result.getLong(1);
            String identifier = result.getString(2);
            long experimentId = result.getLong(3);
            Experiment experiment = Experiment.findById(experimentId);
            final TaskReference reference = new TaskReference(TypeName.parse(identifier), experiment);
            reference.setId(id);
            return reference;
        } catch (SQLException e) {
            throw new XPMRuntimeException(e, "Could not construct task reference from DB");
        }
    }


    /**
     * Iterator on experiments
     */
    static public CloseableIterable<TaskReference> find(Experiment experiment) throws SQLException {
        final DatabaseObjects<TaskReference> references = Scheduler.get().taskReferences();
        return references.find(SELECT_BEGIN + " WHERE experiment = ?", st -> st.setLong(1, experiment.getId()));
    }

    public Collection<TaskReference> getChildren() {
        if (children == null) {
            ArrayList<TaskReference> list = getHierarchy(false);
            children = list;
        }
        return children;
    }

    public Collection<TaskReference> getParents() {
        if (parents == null) {
            ArrayList<TaskReference> list = getHierarchy(true);
            parents = list;
        }

        return parents;
    }

    private ArrayList<TaskReference> getHierarchy(boolean parents) {
        final DatabaseObjects<TaskReference> references = Scheduler.get().taskReferences();

        ArrayList<TaskReference> list = new ArrayList<>();

        try {
            references.find(SELECT_BEGIN + ", ExperimentHierarchy WHERE id = "
                    + (parents ? "parent" : "child") + " AND " + (parents ? "child" : "parent") + " = ?", st -> st.setLong(1, this.getId()))
                    .forEach(list::add);
        } catch (SQLException e) {
            throw new XPMRuntimeException(e);
        } catch (WrappedException e) {
            if (!(e.getCause() instanceof CloseException)) {
                throw e;
            }
        }
        return list;
    }

    public List<Resource> getResources() {
        if (resources != null) {
            ArrayList<Resource> list = new ArrayList<>();
            final DatabaseObjects<Resource> resources = Scheduler.get().resources();
            try {
                resources.find(Resource.SELECT_BEGIN + ", ExperimentResources WHERE id=resource AND task=?", st -> st.setLong(1, this.getId()))
                        .forEach(list::add);
            } catch (SQLException e) {
                throw new XPMRuntimeException(e);
            } catch (WrappedException e) {
                if (!(e.getCause() instanceof CloseException)) {
                    throw e;
                }
            }
            this.resources = list;
        }
        return resources;
    }
}
