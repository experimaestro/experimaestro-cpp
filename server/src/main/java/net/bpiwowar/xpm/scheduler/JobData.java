package net.bpiwowar.xpm.scheduler;

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
import net.bpiwowar.xpm.utils.db.SQLInsert;
import net.bpiwowar.xpm.utils.log.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import static java.lang.String.format;

/**
 * Data associated to a job
 */
public class JobData {
    final static public Logger LOGGER = Logger.getLogger();

    static final SQLInsert SQL_INSERT = new SQLInsert("Jobs", false, "id", "priority", "submitted", "start", "end", "unsatisfied", "holding");

    private final Job job;

    private long startTimestamp;

    private long endTimestamp;

    private int nbUnsatisfied = 0;

    private int nbHolding = 0;

    private int priority;

    private long timestamp = System.currentTimeMillis();

    public JobData(Job job) {
        this.job = job;
        if (job.getId() != null) {
            try (PreparedStatement st = Scheduler.prepareStatement("SELECT priority, submitted, start, end, unsatisfied, holding FROM Jobs WHERE id=?")) {
                st.setLong(1, job.getId());
                st.execute();
                final ResultSet rs = st.getResultSet();
                if (!rs.next()) {
                    throw new XPMRuntimeException("No Job data for %s", job);
                }

                priority = rs.getInt(1);

                timestamp = rs.getTimestamp(2).getTime();
                startTimestamp = rs.getTimestamp(3).getTime();
                endTimestamp = rs.getTimestamp(4).getTime();

                nbUnsatisfied = rs.getInt(5);
                nbHolding = rs.getInt(6);

                LOGGER.debug("Retrieved job [%s] data: %s", job, this);
            } catch (SQLException e) {
                throw new XPMRuntimeException(e, "Could not load data");
            }
        }
    }

    @Override
    public String toString() {
        return format("submitted=%d, start=%d, end=%d, unsatisfied=%d, holding=%d",
                 timestamp, startTimestamp, endTimestamp, nbUnsatisfied, nbHolding);
    }

    public void save(boolean update, long id) throws SQLException {
        JobData.SQL_INSERT.execute(Scheduler.getConnection(), update, id, getPriority(), new Timestamp(getTimestamp()),
                new Timestamp(getStartTimestamp()), new Timestamp(getEndTimestamp()), getNbUnsatisfied(), getNbHolding());
    }

    /**
     * When did the job start (0 if not started)
     */
    public long getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(long startTimestamp) {
        if (startTimestamp != this.startTimestamp) {
            updateValue(new Timestamp(startTimestamp), "start");
        }
        this.startTimestamp = startTimestamp;
    }

    /**
     * When did the job stop (0 when it did not stop yet)
     */
    public long getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(long endTimestamp) {
        if (endTimestamp != this.endTimestamp) {
            updateValue(new Timestamp(endTimestamp), "end");
        }
        this.endTimestamp = endTimestamp;
    }

    /**
     * Number of unsatisfied jobs
     */
    public int getNbUnsatisfied() {
        return nbUnsatisfied;
    }

    /**
     * Number of holding jobs
     */
    public int getNbHolding() {
        return nbHolding;
    }

    public boolean setRequired(int nbUnsatisfied, int nbHolding) {
        if (nbUnsatisfied != this.nbUnsatisfied || nbHolding != this.nbHolding) {
            if (job.inDatabase()) try {
                Scheduler.statement("UPDATE Jobs SET unsatisfied=?, holding=? WHERE id=?")
                        .setInt(1, nbUnsatisfied).setInt(2, nbHolding).setLong(3, job.getId())
                        .execute().close();
                LOGGER.debug("Updated job %s: unsatisfied=%d, holding=%d", job, nbUnsatisfied, nbHolding);
            } catch (SQLException e) {
                throw new XPMRuntimeException(e, "Could not set statistics in database");
            }
            this.nbHolding = nbHolding;
            this.nbUnsatisfied = nbUnsatisfied;
            return true;
        }
        return false;
    }


    protected void updateValue(Object object, String sqlField) {
        if (job.inDatabase()) try {
            Scheduler.statement(format("UPDATE Jobs SET %s=? WHERE id=?", sqlField))
                    .setObject(1, object).setLong(2, job.getId())
                    .execute().close();
        } catch (SQLException e) {
            throw new XPMRuntimeException(e, "Could not set value in database");
        }
    }

    /**
     * The priority of the job (the higher, the more urgent)z
     */
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        if (priority != this.priority) {
            updateValue(priority, "priority");
        }
        this.priority = priority;
    }

    /**
     * When was the job submitted (in case the priority is not enough)
     */
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        if (timestamp != this.timestamp) {
            updateValue(timestamp, "timestamp");
        }
        this.timestamp = timestamp;
    }
}
