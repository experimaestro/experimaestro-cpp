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

import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.scripting.Exposed;
import sf.net.experimaestro.scheduler.DatabaseObjects;
import sf.net.experimaestro.scheduler.Identifiable;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.CloseableIterable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;

/**
 * An experiment
 */
@Exposed
public class Experiment implements Identifiable {
    /**
     * Experiment unique identifier
     */
    Long id;

    /**
     * Tasks
     */
    Collection<TaskReference> tasks = new ArrayList<>();

    /**
     * Timestamp
     */
    private long timestamp;

    /**
     * String identifier
     */
    String identifier;

    /**
     * Scheduler
     */
    transient private Scheduler scheduler;

    protected Experiment() {
    }

    /**
     * New task
     *
     * @param identifier The experiment taskId
     */
    public Experiment(String identifier, long timestamp) {
        this.identifier = identifier;
        this.timestamp = timestamp;
    }

    public void init(Scheduler scheduler) {
        this.scheduler = scheduler;
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
        DatabaseObjects<Experiment> shares = Scheduler.get().experiments();
        shares.save(this, "INSERT INTO Experiments(name, timestamp) VALUES(?, ?)", st -> {
            st.setString(1, identifier);
            st.setTimestamp(2, new Timestamp(timestamp));
        });
    }


    public static Experiment create(DatabaseObjects<Experiment> db, ResultSet result) {

        try {
            String identifier = result.getString(1);
            long timestamp = result.getTimestamp(2).getTime();

            final Experiment experiment = new Experiment(identifier, timestamp);
            return experiment;
        } catch (SQLException e) {
            throw new XPMRuntimeException(e, "Could not construct network share");
        }
    }

    static private final String SELECT_BEGIN = "SELECT id, name, timestamp FROM Experiments";

    /**
     * Iterator on experiments
     */
    static public CloseableIterable<Experiment> experiments() throws SQLException {
        final DatabaseObjects<Experiment> experiments = Scheduler.get().experiments();
        return experiments.find(SELECT_BEGIN, st -> {});
    }
}
