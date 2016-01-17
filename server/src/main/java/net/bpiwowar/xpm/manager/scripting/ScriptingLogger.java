package net.bpiwowar.xpm.manager.scripting;

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

import org.apache.log4j.Level;
import net.bpiwowar.xpm.utils.JSUtils;
import net.bpiwowar.xpm.utils.Lazy;
import net.bpiwowar.xpm.utils.log.Logger;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Exposed
public class ScriptingLogger extends WrapperObject<Logger> {
    public ScriptingLogger(Logger logger) {
        super(logger);
    }

    @Expose
    public ScriptingLogger(String name) {
        // When generating documentation, the script context is not defined
        super(ScriptContext.get() == null ? null : ScriptContext.get().getLogger(name));
    }

//    private void log(Level level, Scriptable thisObj, Object[] args, Function funObj) {
//        if (args.length < 1)
//            throw new XPMRuntimeException("There should be at least one argument when logging");
//
//        String format = Context.toString(args[0]);
//        Object[] objects = new Object[args.length - 1];
//        for (int i = 1; i < args.length; i++)
//            objects[i - 1] = JSUtils.unwrap(args[i]);
//
//        object.log(level, format, objects);
//    }

    @Expose("trace")
    public void trace(Object format, Object... objects) {
        object.trace(Lazy.format(JSUtils.toString(format), objects));
    }

    @Expose("debug")
    public void debug(Object format, Object... objects) {
        object.debug(Lazy.format(JSUtils.toString(format), objects));
    }

    @Expose("info")
    public void info(Object format, Object... objects) {
        object.info(Lazy.format(JSUtils.toString(format), objects));
    }

    @Expose("warn")
    public void warn(Object format, Object... objects) {
        object.warn(Lazy.format(JSUtils.toString(format), objects));
    }

    @Expose("error")
    public void error(Object format, Object... objects) {
        object.error(Lazy.format(JSUtils.toString(format), objects));
    }

    @Expose("fatal")
    public void fatal(Object format, Object... objects) {
        object.fatal(Lazy.format(JSUtils.toString(format), objects));
    }

    @Expose("create")
    @Help(value = "Creates a new object with the given name")
    public ScriptingLogger create(String subname) {
        return new ScriptingLogger(object.getName() + "." + subname);
    }

    @Expose("set_level")
    @Help("Sets the level")
    public void set_level(@Argument(type = "String", name = "level") String level) {
        object.setLevel(Level.toLevel(level));
    }

}
