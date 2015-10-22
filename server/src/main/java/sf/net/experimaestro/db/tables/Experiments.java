package sf.net.experimaestro.db.tables;

import com.google.common.collect.ImmutableSet;
import sf.net.experimaestro.db.Db;
import sf.net.experimaestro.db.Default;
import sf.net.experimaestro.db.Field;
import sf.net.experimaestro.db.Identity;
import sf.net.experimaestro.db.Index;
import sf.net.experimaestro.db.MaxSize;
import sf.net.experimaestro.db.NotNull;
import sf.net.experimaestro.db.Query;
import sf.net.experimaestro.db.Table;

import java.sql.Timestamp;
import java.util.Set;

/**
 * Experiments table
 */
public class Experiments extends Table {
    static final public Experiments T = new Experiments();

    @Identity()
    static Field<Long> ID;

    @NotNull
    @MaxSize(256)
    static Field<String> NAME;

    @NotNull
    @Default("CURRENT_TIMESTAMP")
    static Field<Timestamp> TIMESTAMP;


    @Index("name")
    Set<Field> es = ImmutableSet.of(T.NAME, TIMESTAMP);

    /** Select id, name and timestamp */
    static Query SELECT = Db.select(ID, NAME, TIMESTAMP).orderby(TIMESTAMP);

}
