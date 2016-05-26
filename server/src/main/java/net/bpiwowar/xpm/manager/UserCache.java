package net.bpiwowar.xpm.manager;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.manager.json.Json;
import net.bpiwowar.xpm.scheduler.Scheduler;
import net.bpiwowar.xpm.scheduler.XPMResultSet;
import net.bpiwowar.xpm.utils.GsonConverter;
import net.bpiwowar.xpm.utils.JsonSerializationInputStream;
import net.bpiwowar.xpm.utils.MessageDigestWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 *
 */
public class UserCache {
    /**
     * Store some data
     *
     * @param id       The id
     * @param validity The validity in seconds
     * @param key      The JSON key
     * @param value    The json value
     */
    public static void store(String id, long validity, Json key, Json value) throws NoSuchAlgorithmException, IOException, SQLException {
        MessageDigestWriter writer = new MessageDigestWriter(Charset.forName("UTF-8"), "MD5");
        key.write(writer);
        final String md5 = writer.getHexDigest();

        Scheduler.statement("DELETE FROM UserCache WHERE identifier=? and keyhash=?")
                .setString(1, id)
                .setString(2, md5)
                .executeUpdate();


        int count = Scheduler.statement("INSERT INTO UserCache(identifier, keyhash, validity, jsonkey, jsondata) VALUES(?, ?, ?, ?, ?)")
                .setString(1, id)
                .setString(2, md5)
                .setTimestamp(3, new Timestamp(System.currentTimeMillis() + validity * 1000))
                .setBlob(4, JsonSerializationInputStream.of(key, GsonConverter.defaultBuilder))
                .setBlob(5, JsonSerializationInputStream.of(value, GsonConverter.defaultBuilder))
                .executeUpdate();

        if (count != 1) throw new SQLException("Could not set the value in cache");
    }

    /**
     * Retrieve a value from cache
     *
     * @param id
     * @param key
     * @return
     * @throws SQLException
     */
    public static Json retrieve(String id, Json key) throws SQLException, IOException, NoSuchAlgorithmException {
        Scheduler.statement("DELETE FROM UserCache WHERE validity < ?")
                .setTimestamp(1, new Timestamp(System.currentTimeMillis()))
                .executeUpdate();

        MessageDigestWriter writer = new MessageDigestWriter(Charset.forName("UTF-8"), "MD5");
        key.write(writer);
        final String md5 = writer.getHexDigest();

        try (final XPMResultSet rs = Scheduler.statement("SELECT jsonkey, jsondata FROM UserCache WHERE identifier=? and keyhash=?")
                .setString(1, id)
                .setString(2, md5)
                .execute()
                .singleResultSet(true)) {
            if (rs == null) return null;

            try (final InputStream is1 = rs.getBinaryStream(1);
                 final InputStream is2 = rs.getBinaryStream(2)) {
                final Gson gson = GsonConverter.defaultBuilder.create();

                Json dbkey = getJson(is1, gson);
                if (!dbkey.equals(key)) {
                    // MD5 collision, return null
                    return null;
                }

                Json value = getJson(is2, gson);

                return value;
            }
        }


    }

    private static Json getJson(InputStream is1, Gson gson) {
        try (Reader reader = new InputStreamReader(is1); JsonReader jsonReader = new JsonReader(reader)) {
            return gson.fromJson(jsonReader, Json.class);
        } catch (IOException e) {
            throw new XPMRuntimeException(e, "Error while deserializing JSON");
        }
    }

}
