/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.manager.js;

import org.apache.log4j.Level;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.annotations.JSFunction;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.Lazy;
import sf.net.experimaestro.utils.log.Logger;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 26/11/12
 */
public class JSLogger extends JSObject {
    private Logger logger;
    private XPMObject xpm;


    public JSLogger(XPMObject xpm, String name) {
        this.xpm = xpm;
        logger = Logger.getLogger(xpm.loggerRepository, name);
    }

    public void jsConstructor(Scriptable _xpm, String name) {
        xpm = (XPMObject) ((NativeJavaObject) _xpm).unwrap();
        logger = Logger.getLogger(xpm.loggerRepository, name);
    }


    @JSFunction("trace")
    @JSHelp(value = "", arguments = @JSArguments({
            @JSArgument(type = "String", name = "format", help = "The format string"),
            @JSArgument(type = "Object...", name = "objects")
    })
    )
    static public void jsFunction_trace(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        log(Level.TRACE, cx, thisObj, args, funObj);
    }

    public void debug(String format, Object... objects) {
        logger.info(Lazy.format(format, objects));
    }

    public void info(String format, Object... objects) {
        logger.info(Lazy.format(format, objects));
    }

    @JSHelp(value = "", arguments = @JSArguments({
            @JSArgument(type = "String", name = "format", help = "The format string"),
            @JSArgument(type = "Object...", name = "objects")
    })
    )
    static public void jsFunction_warn(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        log(Level.WARN, cx, thisObj, args, funObj);
    }

    @JSFunction("error")
    @JSHelp(value = "", arguments = @JSArguments({
            @JSArgument(type = "String", name = "format", help = "The format string"),
            @JSArgument(type = "Object...", name = "objects")
    })
    )
    static public void jsFunction_error(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        log(Level.ERROR, cx, thisObj, args, funObj);
    }

    @JSFunction("fatal")
    @JSHelp(value = "", arguments = @JSArguments({
            @JSArgument(type = "String", name = "format", help = "The format string"),
            @JSArgument(type = "Object...", name = "objects")
    })
    )
    static public void jsFunction_fatal(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        log(Level.FATAL, cx, thisObj, args, funObj);
    }

    @JSFunction("create")
    @JSHelp(value = "Creates a new logger with the given name", arguments = @JSArguments(@JSArgument(type="String", name="name")))
    static public Scriptable jsFunction_create(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        if (args.length != 1)
            throw new ExperimaestroRuntimeException("Logger.create() expects one argument, got %d", args.length);

        final JSLogger jslogger = (JSLogger) thisObj;
        XPMObject xpm = jslogger.xpm;
        final String subname = JSUtils.toString(args[0]);
        return xpm.newObject(JSLogger.class, xpm, jslogger.logger.getName() + "." + subname);
    }

    @JSFunction("set_level")
    @JSHelp("Sets the level")
    public void set_level(@JSArgument(type="String", name="level") String level) {
        logger.setLevel(Level.toLevel(level));
    }

    static private void log(Level level, Context cx, Scriptable thisObj, Object[] args, Function funObj) {
        if (args.length < 1)
            throw new ExperimaestroRuntimeException("There should be at least one argument when logging");

        String format = Context.toString(args[0]);
        Object[] objects = new Object[args.length - 1];
        for (int i = 1; i < args.length; i++)
            objects[i - 1] = JSUtils.unwrap(args[i]);

        ((JSLogger) thisObj).logger.log(level, format, objects);
    }


}
