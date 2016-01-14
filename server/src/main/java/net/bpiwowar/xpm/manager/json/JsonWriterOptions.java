package net.bpiwowar.xpm.manager.json;

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

import com.google.common.collect.ImmutableSet;
import net.bpiwowar.xpm.manager.Constants;
import net.bpiwowar.xpm.manager.QName;

import java.nio.file.Path;
import java.util.Set;
import java.util.function.Function;

/**
 * Created by bpiwowar on 3/10/14.
 */
public class JsonWriterOptions {
    /**
     * Default set of ignored options
     */
    public static final Set<QName> DEFAULT_IGNORE = ImmutableSet.of(Constants.XP_RESOURCE_TYPE, Constants.XP_PATH, Constants.XP_FILE, Constants.XP_DIRECTORY);
    public Set<QName> ignore = DEFAULT_IGNORE;
    public static final JsonWriterOptions DEFAULT_OPTIONS = new JsonWriterOptions();
    public boolean simplifyValues = true;
    public boolean ignore$ = true;
    public boolean ignoreNull = true;
    Function<Path, String> resolver = f -> f.toString();
    public JsonWriterMode mode = JsonWriterMode.DEFAULT;

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

    public JsonWriterOptions resolveFile(Function<Path, String> resolver) {
        this.resolver = resolver;
        return this;
    }

    public JsonWriterOptions mode(JsonWriterMode mode) {
        this.mode = mode;
        return this;
    }
}
