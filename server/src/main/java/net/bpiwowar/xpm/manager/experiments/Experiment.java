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
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.scheduler.DatabaseObjects;
import net.bpiwowar.xpm.scheduler.Identifiable;
import net.bpiwowar.xpm.scheduler.Resource;
import net.bpiwowar.xpm.scheduler.Scheduler;
import net.bpiwowar.xpm.utils.CloseableIterable;
import net.bpiwowar.xpm.utils.log.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/**
 * An experiment
 */
@Exposed
public class Experiment implements Identifiable {
    static private final Logger LOGGER = Logger.getLogger();
    static private final String SELECT_BEGIN = "SELECT id, name, timestamp FROM Experiments";

    /**
     * Experiment unique identifier
     */
    Long id;

    /**
     * Tasks
     */
    List<TaskReference> tasks;

    /**
     * Timestamp
     */
    private long timestamp;

    /**
     * String identifier
     */
    String identifier;

    private Experiment(long id, String identifier, long timestamp) {
        this.id = id;
        this.identifier = identifier;
        this.timestamp = timestamp;
    }

    /**
     * New task
     *
     * @param identifier The experiment taskId
     */
    public Experiment(String identifier, long timestamp) {
        this.identifier = identifier;
        this.timestamp = timestamp;
        this.tasks = new ArrayList<>();
    }

    @Override
    public boolean inDatabase() {
        return id == null;
    }

    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return identifier;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void save() throws SQLException {
        DatabaseObjects<Experiment> experiments = Scheduler.get().experiments();
        experiments.save(this, "INSERT INTO Experiments(name, timestamp) VALUES(?, ?)", st -> {
            st.setString(1, identifier);
            st.setTimestamp(2, new Timestamp(timestamp));
        });
    }


    public static Experiment create(DatabaseObjects<Experiment> db, ResultSet result) {

        try {
            long id = result.getLong(1);
            String identifier = result.getString(2);
            long timestamp = result.getTimestamp(3).getTime();

            final Experiment experiment = new Experiment(id, identifier, timestamp);
            return experiment;
        } catch (SQLException e) {
            throw new XPMRuntimeException(e, "Could not load experiment from DB");
        }
    }


    /**
     * Iterator on experiments
     */
    static public CloseableIterable<Experiment> experiments() throws SQLException {
        final DatabaseObjects<Experiment> experiments = Scheduler.get().experiments();
        return experiments.find(SELECT_BEGIN + " ORDER BY timestamp DESC", st -> {
        });
    }

    /**
     * Add a new resource to this experiment
     *
     * @param resource The resource to add
     */
    public void add(Resource resource) throws SQLException {
        DatabaseObjects<Experiment> experiments = Scheduler.get().experiments();
        experiments.save(this, "INSERT INTO ExperimentTasks(id, identifier, experiment, parent) VALUES(?, ?, ?, ?)", st -> {
            st.setLong(1, id);

        });
    }

    public static Experiment findById(long id) throws SQLException {
        final DatabaseObjects<Experiment> experiments = Scheduler.get().experiments();
        final Experiment fromCache = experiments.getFromCache(id);
        if (fromCache != null) {
            return fromCache;
        }
        final String query = format("%s WHERE id=?", SELECT_BEGIN);
        return experiments.findUnique(query, st -> st.setLong(1, id));
    }

    public List<TaskReference> getTasks() {
        if (tasks == null) {
            final ArrayList<TaskReference> _tasks = new ArrayList<>();

            try (CloseableIterable<TaskReference> list = TaskReference.find(this)) {
                list.forEach(_tasks::add);
            } catch (SQLException e) {
                throw new XPMRuntimeException(e);
            } catch (CloseException e) {
                LOGGER.error(e, "Error while closing the iterator");
            }
            tasks = _tasks;
        }
        return tasks;
    }

    public void add(TaskReference taskReference) {
        tasks.add(taskReference);
    }

    public static Experiment findByIdentifier(String name) throws SQLException {
        final String query = format("%s WHERE identifier=?", SELECT_BEGIN);
        final DatabaseObjects<Experiment> experiments = Scheduler.get().experiments();
        return experiments.findUnique(query, st -> st.setString(1, name));
    }

    public CloseableIterable<Resource> resources() throws SQLException {
        final DatabaseObjects<Resource> resources = Scheduler.get().resources();
        return resources.find(Resource.SELECT_BEGIN + ", ExperimentResources WHERE id=resource AND experiment=?",
                st -> st.setLong(1, this.getId()));
    }
}
