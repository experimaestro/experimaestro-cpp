package net.bpiwowar.xpm.manager.scripting;

import com.google.common.collect.ImmutableMap;
import net.bpiwowar.xpm.manager.TypeName;
import net.bpiwowar.xpm.manager.json.Json;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.xml.namespace.NamespaceContext;

/**
 * Test for class description
 */
public class ClassDescriptionTest {

    @Exposed
    static public class A {
        @Expose
        public static int a(String ignored) {
            return 0;
        }
    }

    @Exposed
    static public class B extends A {
        @Expose
        public static int a(Integer ignored) {
            return 1;
        }
    }

    @Test()
    public static void inheritance() {
        final ClassDescription bclass = ClassDescription.analyzeClass(B.class);
        int result = (int) bclass.getMethod("a").call(new FakeLanguageContext(), new B(), ImmutableMap.of(), "hello");
        Assert.assertEquals(result, 0, "Wrong method called");

        result = (int) bclass.getMethod("a").call(new FakeLanguageContext(), new B(), ImmutableMap.of(), 1.2);
        Assert.assertEquals(result, 0, "Wrong method called");

        result = (int) bclass.getMethod("a").call(new FakeLanguageContext(), new B(), ImmutableMap.of(), 1);
        Assert.assertEquals(result, 1, "Wrong method called");
    }

    private static class FakeLanguageContext extends LanguageContext {
        @Override
        public Json toJSON(Object object) {
            return null;
        }

        @Override
        public NamespaceContext getNamespaceContext() {
            return null;
        }

        @Override
        public RuntimeException runtimeException(Exception e, String format, Object... objects) {
            return null;
        }

        @Override
        public TypeName qname(Object value) {
            return null;
        }

        @Override
        public Object toJava(Object value) {
            return value;
        }

        @Override
        public ScriptLocation getScriptLocation() {
            return null;
        }
    }
}