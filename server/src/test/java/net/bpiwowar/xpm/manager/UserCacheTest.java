package net.bpiwowar.xpm.manager;

import com.google.gson.JsonParser;
import net.bpiwowar.xpm.manager.json.Json;
import net.bpiwowar.xpm.utils.XPMEnvironment;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

import static net.bpiwowar.xpm.manager.UserCache.retrieve;
import static net.bpiwowar.xpm.manager.UserCache.store;

/**
 *
 */
public class UserCacheTest extends XPMEnvironment {
    public UserCacheTest() throws Throwable {
        prepare();
    }

    @Test
    public void simple() throws NoSuchAlgorithmException, SQLException, IOException {
        String id1 = "xpm.id1";
        String id2 = "xpm.id2";

        Json key1 = Json.toJSON(new JsonParser().parse("{\"a\": 1 }"));
        Json key2 = Json.toJSON(new JsonParser().parse("{\"a\": 2 }"));

        Json value1 = Json.toJSON(new JsonParser().parse("{\"b\": 1 }"));
        Json value2 = Json.toJSON(new JsonParser().parse("{\"b\": 2 }"));
        Json value3 = Json.toJSON(new JsonParser().parse("{\"b\": 3 }"));

        store(id1, 1000, key1, value1);
        store(id2, 1000, key1, value2);
        store(id2, 1000, key2, value3);

        Assert.assertEquals(retrieve(id1, key1), value1);
        Assert.assertEquals(retrieve(id2, key1), value2);
        Assert.assertEquals(retrieve(id2, key2), value3);
        Assert.assertNull(retrieve(id1, key2));
    }

    @Test
    public void forget() throws NoSuchAlgorithmException, SQLException, IOException {
        String id1 = "xpm.id1";

        Json key1 = Json.toJSON(new JsonParser().parse("{\"a\": 1 }"));

        Json value1 = Json.toJSON(new JsonParser().parse("{\"b\": 1 }"));

        store(id1, 0, key1, value1);

        final Json retrieved = retrieve(id1, key1);

        Assert.assertNull(retrieved);
    }
}
