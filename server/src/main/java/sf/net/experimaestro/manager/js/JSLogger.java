package sf.net.experimaestro.manager.js;

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
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.Lazy;
import sf.net.experimaestro.utils.log.Logger;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 26/11/12
 */
public class JSLogger extends JSBaseObject {
    private Logger logger;
    private XPMObject xpm;

    @JSFunction
    public JSLogger(XPMObject xpm, String name) {
        this.xpm = xpm;
        logger = Logger.getLogger(xpm.loggerRepository, name);
    }

    static private void log(Level level, Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        if (args.length < 1)
            throw new XPMRuntimeException("There should be at least one argument when logging");

        String format = Context.toString(args[0]);
        Object[] objects = new Object[args.length - 1];
        for (int i = 1; i < args.length; i++)
            objects[i - 1] = JSUtils.unwrap(args[i]);

        ((JSLogger) thisObj).logger.log(level, format, objects);
    }

    @JSFunction("trace")
    public void trace(Object format, Object... objects) {
        logger.trace(Lazy.format(JSUtils.toString(format), objects));
    }

    @JSFunction("debug")
    public void debug(Object format, Object... objects) {
        logger.debug(Lazy.format(JSUtils.toString(format), objects));
    }

    @JSFunction("info")
    public void info(Object format, Object... objects) {
        logger.info(Lazy.format(JSUtils.toString(format), objects));
    }

    @JSFunction("warn")
    public void warn(Object format, Object... objects) {
        logger.warn(Lazy.format(JSUtils.toString(format), objects));
    }

    @JSFunction("error")
    public void error(Object format, Object... objects) {
        logger.error(Lazy.format(JSUtils.toString(format), objects));
    }

    @JSFunction("fatal")
    public void fatal(Object format, Object... objects) {
        logger.fatal(Lazy.format(JSUtils.toString(format), objects));
    }

    @JSFunction("create")
    @JSHelp(value = "Creates a new logger with the given name")
    public Scriptable create(String subname) {
        return xpm.newObject(JSLogger.class, xpm, logger.getName() + "." + subname);
    }

    @JSFunction("set_level")
    @JSHelp("Sets the level")
    public void set_level(@JSArgument(type = "String", name = "level") String level) {
        logger.setLevel(Level.toLevel(level));
    }


}
