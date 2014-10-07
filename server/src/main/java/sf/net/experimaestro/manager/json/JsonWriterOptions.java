package sf.net.experimaestro.manager.json;

import com.google.common.collect.ImmutableSet;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.ValueType;

import java.util.Set;

/**
* Created by bpiwowar on 3/10/14.
*/
public class JsonWriterOptions {
    /** Default set of ignored options */
    public static final Set<QName> DEFAULT_IGNORE = ImmutableSet.of(ValueType.XP_RESOURCE, ValueType.XP_FILE);
    public static final JsonWriterOptions DEFAULT_OPTIONS = new JsonWriterOptions();

    public Set<QName> ignore = DEFAULT_IGNORE;
    public boolean simplifyValues = true;
    public boolean ignore$ = true;

    public JsonWriterOptions(Set<QName> ignore) {
        this.ignore = ignore;
    }

    protected JsonWriterOptions() {
    }

    public JsonWriterOptions ignore$(boolean ignore$) {
        this.ignore$ = ignore$;
        return this;
    }

    public JsonWriterOptions simplifyValues(boolean simplifyValues) {
        this.simplifyValues = simplifyValues;
        return this;
    }
}
