package net.bpiwowar.xpm.manager;

import net.bpiwowar.xpm.manager.json.JsonBoolean;
import net.bpiwowar.xpm.manager.json.JsonInteger;
import net.bpiwowar.xpm.manager.json.JsonObject;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 */
public class JsonSignatureTest {

    @Test
    public void testSimple() throws Exception {
        final JsonObject object = new JsonObject();
        final JsonInteger value = new JsonInteger(1);
        object.put("x", value);

        final JsonObject object2 = new JsonObject();
        object2.put("x", new JsonInteger(1).toJsonObject());

        Assert.assertEquals(JsonSignature.getDescriptor(object), JsonSignature.getDescriptor(object2));
    }

    @Test
    public void testDefault() throws Exception {
        final JsonObject object = new JsonObject();
        final JsonInteger value = new JsonInteger(1);
        object.put("x", value.annotate(Constants.JSON_KEY_DEFAULT, JsonBoolean.TRUE));

        Assert.assertEquals(JsonSignature.getDescriptor(object), "{}");
    }

}