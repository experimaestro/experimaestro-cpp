package net.bpiwowar.xpm.manager.json;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test for JSON
 */
public class JsonTest {

    @Test(description = "Verify that a sealed object returns itself when copied")
    public void sealCopyTest() {
        final JsonObject object = new JsonObject();

        Assert.assertTrue(object.copy(false) != object, "Non sealed object returns itself");
        Assert.assertTrue(object.seal().copy(false) == object, "Sealed object did not return itself");

    }
}