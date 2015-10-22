package sf.net.experimaestro.db;

import java.sql.Timestamp;

/**
 * Experiments table
 */
public class Experiments {
    @Identity
    Field<Long> id;


    @NotNull
    @MaxSize(256)
    @Indexes(@Index("experiment_name"))
    Field<String> name;


    @NotNull
    @Default("CURRENT_TIMESTAMP")
    @Indexes(@Index("experiment_name"))
    Field<Timestamp> timestamp;
}
