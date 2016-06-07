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

import it.unimi.dsi.fastutil.longs.LongRBTreeSet;
import net.bpiwowar.xpm.exceptions.CloseException;
import net.bpiwowar.xpm.exceptions.WrappedSQLException;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.scheduler.DatabaseObjects;
import net.bpiwowar.xpm.scheduler.Identifiable;
import net.bpiwowar.xpm.scheduler.Message;
import net.bpiwowar.xpm.scheduler.Resource;
import net.bpiwowar.xpm.scheduler.ResourceState;
import net.bpiwowar.xpm.scheduler.Scheduler;
import net.bpiwowar.xpm.scheduler.XPMResultSet;
import net.bpiwowar.xpm.scheduler.XPMStatement;
import net.bpiwowar.xpm.utils.CloseableIterable;
import net.bpiwowar.xpm.utils.log.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        DatabaseObjects<Experiment, Void> experiments = Scheduler.get().experiments();

        // Update the last flag
        Scheduler.statement("UPDATE Experiments SET last = FALSE WHERE name=?")
                .setString(1, identifier)
                .executeUpdate();

        experiments.save(this, "INSERT INTO Experiments(name, timestamp, last) VALUES(?, ?, TRUE)", st -> {
            st.setString(1, identifier);
            st.setTimestamp(2, new Timestamp(timestamp));
        });

        // Notify listeners
        new ExperimentMessage(Message.Event.EXPERIMENT_ADDED, this).send();
    }


    public static Experiment create(DatabaseObjects<Experiment, Void> db, ResultSet result, Void ignored) {

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
     *
     * @param latest
     */
    static public CloseableIterable<Experiment> experiments(boolean latest) throws SQLException {
        final DatabaseObjects<Experiment, Void> experiments = Scheduler.get().experiments();
        String query = latest ? "id, name, timestamp FROM Experiments WHERE last == true"
                : "SELECT id, name, timestamp FROM Experiments ORDER BY timestamp DESC";
        return experiments.find(query, st -> {
        });
    }

    /**
     * Add a new resource to this experiment
     *
     * @param resource The resource to add
     */
    public void add(Resource resource) throws SQLException {
        DatabaseObjects<Experiment, Void> experiments = Scheduler.get().experiments();
        experiments.save(this, "INSERT INTO ExperimentTasks(id, identifier, experiment, parent) VALUES(?, ?, ?, ?)", st -> {
            st.setLong(1, id);

        });
    }

    public static Experiment findById(long id) throws SQLException {
        final DatabaseObjects<Experiment, Void> experiments = Scheduler.get().experiments();
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
        final String query = format("%s WHERE name=? ORDER BY timestamp DESC LIMIT 1", SELECT_BEGIN);
        final DatabaseObjects<Experiment, Void> experiments = Scheduler.get().experiments();
        return experiments.findUnique(query, st -> st.setString(1, name));
    }


    public static Experiment find(String identifier, long timestamp) throws SQLException {
        final String query = format("%s WHERE name=? AND timestamp=?", SELECT_BEGIN);
        final DatabaseObjects<Experiment, Void> experiments = Scheduler.get().experiments();
        return experiments.findUnique(query, st -> {
            st.setString(1, identifier);
            st.setTimestamp(2, new Timestamp(timestamp));
        });
    }

    public static CloseableIterable<Experiment> findAllByIdentifier(String name) throws SQLException {
        final String query = format("%s WHERE name=? ORDER BY timestamp DESC", SELECT_BEGIN);
        final DatabaseObjects<Experiment, Void> experiments = Scheduler.get().experiments();
        return experiments.find(query, st -> st.setString(1, name));
    }

    /**
     * Returns all resources associated with this experiment
     *
     * @return The resources as an iterable
     * @throws SQLException
     */
    public List<TaskResource> resources() throws SQLException {
        final DatabaseObjects<Resource, Void> resources = Scheduler.get().resources();
        final String query = "SELECT DISTINCT r.id, r.type, r.path, r.status, et.id, et.identifier " +
                "FROM Resources r, ExperimentTasks et, ExperimentResources er " +
                "WHERE er.resource = r.id AND et.id=er.task AND et.experiment=?";
        List<TaskResource> list = new ArrayList<>();
        Scheduler.statement(query)
                .setLong(1, this.getId())
                .stream()
                .forEach(rs -> {
                    try {
                        final Resource resource = resources.getOrCreate(rs.get());
                        list.add(new TaskResource(rs.getLong(5), rs.getString(6), resource));
                    } catch (SQLException e) {
                        throw new WrappedSQLException(e);
                    }
                });
        return list;
    }

    /**
     * Returns all resources associated with this experiment
     *
     * @param identifier
     * @return The resources as an iterable
     * @throws SQLException
     */
    static public List<Resource> resourcesByIdentifier(String identifier, EnumSet<ResourceState> states) throws SQLException {
        final DatabaseObjects<Resource, Void> resources = Scheduler.get().resources();
        final String query = "SELECT DISTINCT r.id, r.type, r.path, r.status, et.id, et.identifier " +
                "FROM Resources r, ExperimentTasks et, ExperimentResources er, Experiments e " +
                "WHERE er.resource = r.id AND et.id=er.task AND et.experiment=e.id AND e.name=? AND r.status in ("
                + states.stream().map(s -> Integer.toString(s.value())).collect(Collectors.joining(","))
                + ")";

        List<Resource> list = new ArrayList<>();
        Scheduler.statement(query)
                .setString(1, identifier)
                .stream()
                .forEach(rs -> {
                    try {
                        final Resource resource = resources.getOrCreate(rs.get());
                        list.add(resource);
                    } catch (SQLException e) {
                        throw new WrappedSQLException(e);
                    }
                });
        return list;
    }

    public static Stream<ExperimentReference> experimentNames() throws WrappedSQLException {
        try {
            return Scheduler.get()
                    .statement("SELECT name, max(timestamp) FROM Experiments GROUP BY name ORDER BY max(timestamp) DESC")
                    .stream()
                    .map(rs -> new ExperimentReference(rs.getString(1), rs.getTimeStamp(2)));
        } catch (SQLException e) {
            throw new WrappedSQLException(e);
        }
    }

    //        public final String query = "SELECT DISTINCT r.id, r.type, r.path, r.status " +
//                "FROM Resources r, ExperimentTasks et, ExperimentResources er, Experiments e " +
//                "WHERE er.resource = r.id AND et.id=er.task AND et.experiment=e.id AND e.identifier=? AND e.timestamp < ?";
    public static long deleteOlder(boolean simulate, ExperimentReference reference) throws SQLException {
        final String query = (simulate ? "SELECT COUNT(*)" : "DELETE")
                + " FROM Experiments WHERE name=? AND timestamp < ?";
        try (XPMStatement st = Scheduler.statement(query)
                .setString(1, reference.identifier)
                .setTimestamp(2, reference.timestamp)) {

            if (simulate) {
                try (final XPMResultSet set = st.singleResultSet()) {
                    return set.getLong(1);
                }
            }

            return st.executeUpdate();
        }
    }

    /**
     * List resources that are neither part of an experiment nor a dependency
     * of a resource part of an experiment
     */
    public static Set<Long> listObsoleteResources() throws SQLException, CloseException {
        // Retrieve resources ids without experiment
//        final String query = "SELECT DISTINCT r.id " +
//                "FROM Resources r LEFT JOIN ExperimentResources er ON er.resource = r.id " +
//                "WHERE er.resource IS NULL ORDER BY r.id";

        // Order the resources by decreasing id so that we can check right
        // away if its dependencies are also obsolete
        final String query = "SELECT DISTINCT r.id, d.toId " +
                "FROM Resources r " +
                "LEFT JOIN Dependencies d ON d.fromId=r.id " +
                "WHERE NOT EXISTS(SELECT * FROM ExperimentResources er WHERE er.resource = r.id) " +
                "ORDER BY r.id DESC";

        LongRBTreeSet set = new LongRBTreeSet((a, b) -> Long.compare(b, a));
        try (final XPMStatement st = Scheduler.statement(query)) {
            st.execute();
            long oldId = -1;
            boolean skip = false;

            try (XPMResultSet rs = st.resultSet()) {
                while (rs.next()) {
                    final long rid = rs.getLong(1);
                    if (rid != oldId) {
                        set.add(rid);
                        oldId = rid;
                        skip = false;
                    } else if (skip) continue;


                    final long toId = rs.getLong(2);
                    if (!rs.wasNull()) {
                        if (toId < rid) throw new AssertionError("This should not happen (DB hypothesis)...");


                        if (!set.contains(toId)) {
                            set.remove(rid);
                            skip = true;
                        }
                    }
                }
            }
        }

        return set;
    }

}
