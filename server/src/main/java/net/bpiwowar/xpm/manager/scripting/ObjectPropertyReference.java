package net.bpiwowar.xpm.manager.scripting;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Access to a property for a given object
 */
@Exposed
public class ObjectPropertyReference implements ScriptingReference {
    private final Object object;
    private Function<Object, Object> getter;
    private BiConsumer<Object, Object> setter;

    public ObjectPropertyReference(Function<Object, Object> getter, BiConsumer<Object, Object> setter, Object object) {
        this.object = object;
        this.getter = getter;
        this.setter = setter;
    }

    @Expose(mode = ExposeMode.PROPERTY)
    public Object get(LanguageContext cx) {
        return getter.apply(object);
    }

    @Override
    public void set(LanguageContext cx, Object value) {
        setter.accept(object, value);
    }
}
