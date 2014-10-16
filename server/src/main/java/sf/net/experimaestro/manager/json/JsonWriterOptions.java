package sf.net.experimaestro.manager.json;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.vfs2.FileObject;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.ValueType;

import java.util.Set;
import java.util.function.Function;

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
    public boolean ignoreNull = true;
    Function<FileObject, String> resolver = f -> f.toString();

    public JsonWriterOptions(Set<QName> ignore) {
        this.ignore = ignore;
    }

    public JsonWriterOptions() {
        this(DEFAULT_IGNORE);
    }

    public JsonWriterOptions ignore$(boolean ignore$) {
        this.ignore$ = ignore$;
        return this;
    }

    public JsonWriterOptions ignoreNull(boolean ignoreNull) {
        this.ignoreNull = ignoreNull;
        return this;
    }

    public JsonWriterOptions simplifyValues(boolean simplifyValues) {
        this.simplifyValues = simplifyValues;
        return this;
    }

    public JsonWriterOptions resolveFile(Function<FileObject, String> resolver) {
        this.resolver = resolver;
        return this;
    }
}
