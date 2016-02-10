package net.bpiwowar.xpm.connectors;

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

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.manager.scripting.Expose;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.scheduler.ConstructorRegistry;
import net.bpiwowar.xpm.scheduler.DatabaseObjects;
import net.bpiwowar.xpm.scheduler.Identifiable;
import net.bpiwowar.xpm.scheduler.Scheduler;
import net.bpiwowar.xpm.utils.GsonConverter;
import net.bpiwowar.xpm.utils.GsonSerialization;
import net.bpiwowar.xpm.utils.JsonSerializationInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;

import static java.lang.String.format;
import static net.bpiwowar.xpm.utils.PathUtils.QUOTED_SPECIAL;
import static net.bpiwowar.xpm.utils.PathUtils.protect;

/**
 * This class represents any layer that can get between a host where files can be stored
 * and possibly where a command can be executed.
 * <p>
 * Connectors are stored in the database so that they can be used
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Exposed
public abstract class Connector implements Comparable<Connector>, Identifiable {
    /**
     * Our registry
     */
    static private ConstructorRegistry<Connector> REGISTRY
            = new ConstructorRegistry(new Class[]{Long.class, String.class})
            .add(SSHConnector.class, LocalhostConnector.class, XPMConnector.class);


    /**
     * Each connector has a unique integer ID
     */
    @GsonSerialization(serialize = false)
    private Long id;

    /**
     * The string identifier
     */
    @GsonSerialization(serialize = false)
    private String identifier;

    /**
     * Whether data has been loaded from database
     */
    transient private boolean dataLoaded;

    public Connector(String identifier) {
        this.identifier = identifier;
    }

    protected Connector() {
    }

    public Connector(long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Connector connector = (Connector) o;

        return identifier.equals(connector.identifier);
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }

    /**
     * Return a new connector from an URI
     */
    @Expose(optional = 1)
    public static Connector create(String uriString, ConnectorOptions options) throws URISyntaxException, SQLException {
        return create(new URI(uriString), options);
    }

    @Expose()
    public Launcher default_launcher() {
        return new DirectLauncher(this);
    }

    public static Connector create(URI uri, ConnectorOptions options) throws SQLException {
        Connector connector;

        switch (uri.getScheme()) {
            case "ssh":
                connector = new SSHConnector(uri, options);
                break;
            case "local":
            case "file":
                connector = Scheduler.get().getLocalhostConnector();
                break;
            default:
                throw new IllegalArgumentException("Unknown connector scheme: " + uri.getScheme());
        }

        Connector dbConnector = Connector.findByURI(connector.getIdentifier());
        return dbConnector == null ? connector : dbConnector;
    }

    /**
     * Retrieves a connector with some requirements
     *
     * @return A valid connector or null if no connector meet the requirements
     */
    public abstract SingleHostConnector getConnector(ComputationalRequirements requirements);

    /**
     * Returns the main connector for this group
     *
     * @return A valid single host connector
     */
    public abstract SingleHostConnector getMainConnector();


//    /**
//     * Create a file with a thread safe mechanism
//     *
//     * @param path
//     * @return A lock object
//     * @throws LockException
//     */
//    public abstract Lock createLockFile(String path) throws LockException;
//

    /**
     * Returns true if the connector can execute commands
     */
    public boolean canExecute() {
        return false;
    }

    /**
     * Returns the connectorId identifier
     */
    public final String getIdentifier() {
        return identifier;
    }

    @Override
    final public int compareTo(Connector other) {
        return identifier.compareTo(other.identifier);
    }

    public abstract Path resolve(String path) throws IOException;

    @Expose
    final public String resolve(Path path) throws IOException {
        return resolve(path, null);
    }

    public String resolve(Path path, Path reference) throws IOException {
        return getMainConnector().resolve(path);
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Save object to database
     *
     * @throws SQLException If something goes wrong
     */
    public void save() throws SQLException {
        save(Scheduler.get().connectors(), null);
    }

    public void save(DatabaseObjects<Connector> connectors, Connector old) throws SQLException {
        try (InputStream jsonInputStream = new JsonSerializationInputStream(out -> {
            try (JsonWriter writer = new JsonWriter(out)) {
                saveJson(writer);
            }
        })) {
            connectors.save(this, "INSERT INTO Connectors(type, uri, data) VALUES(?, ?, ?)", st -> {
                st.setLong(1, DatabaseObjects.getTypeValue(getClass()));
                st.setString(2, getIdentifier());
                st.setBlob(3, jsonInputStream);
            }, old != null);
        } catch (IOException e) {
            throw new XPMRuntimeException(e, "Unexpected I/O error");
        }
    }

    /**
     * Save everything which is not saved in DB on disk
     *
     * @param writer The writer
     */
    protected void saveJson(JsonWriter writer) {
        final Gson gson = GsonConverter.connectorBuilder.create();
        gson.toJson(this, this.getClass(), writer);
    }

    /**
     * Load data from database
     */
    protected void loadData() {
        if (dataLoaded) {
            return;
        }

        Scheduler.get().connectors().loadData(GsonConverter.connectorBuilder, this, "data");
        dataLoaded = true;
    }

    static public Connector create(DatabaseObjects<Connector> db, ResultSet result) {
        try {
            // OK, create connector
            long id = result.getLong(1);
            long type = result.getLong(2);
            String uri = result.getString(3);

            final Constructor<? extends Connector> constructor = REGISTRY.get(type);
            final Connector connector = constructor.newInstance(id, uri);

            return connector;
        } catch (InstantiationException | SQLException | InvocationTargetException | IllegalAccessException e) {
            throw new XPMRuntimeException(e, "Error retrieving database object");
        }
    }

    public static final String SELECT_QUERY = "SELECT id, type, uri FROM Connectors";


    public static Connector findByURI(String uri) throws SQLException {
        final String query = format("%s WHERE uri=?", SELECT_QUERY);
        return Scheduler.get().connectors().findUnique(query, st -> st.setString(1, uri));
    }

    public static Connector findById(long id) throws SQLException {
        final DatabaseObjects<Connector> connectors = Scheduler.get().connectors();
        final Connector fromCache = connectors.getFromCache(id);
        if (fromCache != null) {
            return fromCache;
        }
        final String query = format("%s WHERE id=?", SELECT_QUERY);
        return connectors.findUnique(query, st -> st.setLong(1, id));
    }

    public String quotedPath(Path path) throws IOException {
        return '"' + protect(resolve(path), QUOTED_SPECIAL) + '"';
    }

    public abstract Path defaultTemporaryPath();
}
