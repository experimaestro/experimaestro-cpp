package net.bpiwowar.xpm.manager;

import net.bpiwowar.xpm.manager.json.JsonString;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 *
 */
public class JsonValueTest {

    @Test
    public void testDefault() throws IOException {
        JsonInput input = new JsonInput(Type.XP_STRING);
        input.setDefaultValue(new JsonString("1"));
        final Value value = input.newValue();
        value.set(new JsonString("1"));
        final String descriptor = JsonSignature.getDescriptor(value.get());
        Assert.assertEquals(descriptor, "null");
    }
}