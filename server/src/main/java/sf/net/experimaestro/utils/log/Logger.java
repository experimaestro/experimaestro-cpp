package sf.net.experimaestro.utils.log;

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

import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggerFactory;
import sf.net.experimaestro.utils.io.LoggerPrintStream;

public final class Logger extends org.apache.log4j.Logger {


    private static final String FQCN = Logger.class.getName();
    private static DefaultFactory myFactory = new DefaultFactory();

    public Logger(String name) {
        super(name);
    }

    public final static Factory factory() {
        return myFactory;
    }

    public static Level toLevel(String name, Level level) {
        return Level.toLevel(name, Level.INFO);
    }

    /**
     * This method overrides {@link Logger#getLogger} by supplying its own
     * factory type as a parameter.
     */
    public static Logger getLogger(String name, Factory factory) {
        return (Logger) org.apache.log4j.Logger.getLogger(name, factory);
    }

    public static Logger getLogger(Hierarchy repository) {
        return getLogger(repository, new Throwable().getStackTrace()[1]
                .getClassName());
    }

    public static Logger getLogger(Hierarchy repository, String name) {
        if (repository == null)
            return getLogger(name);
        return (Logger) repository.getLogger(name, myFactory);
    }

    /**
     * This method overrides {@link Logger#getLogger} by supplying its own
     * factory type as a parameter.
     */
    public static Logger getLogger(String name) {
        return getLogger(name, myFactory);
    }

    public static Logger getLogger(Factory factory) {
        return getLogger(new Throwable().getStackTrace()[1]
                .getClassName(), factory);
    }

    public static Logger getLogger() {
        return getLogger(new Throwable().getStackTrace()[1]
                .getClassName(), myFactory);
    }

    public void trace(String format, Object... values) {
        if (repository.isDisabled(Level.TRACE_INT))
            return;

        if (isEnabledFor(Level.TRACE))
            forcedLog(FQCN, Level.TRACE, String.format(format, values), null);
    }

    public void debug(String format, Object... values) {
        if (repository.isDisabled(Level.DEBUG_INT))
            return;
        if (isEnabledFor(Level.DEBUG))
            forcedLog(FQCN, Level.DEBUG, String.format(format, values), null);
    }

    public void debug(Throwable t, String format, Object... values) {
        if (repository.isDisabled(Level.INFO_INT))
            return;
        if (isEnabledFor(Level.INFO))
            forcedLog(FQCN, Level.INFO, String.format(format, values), t);
    }

    public void info(String format, Object... values) {
        if (repository.isDisabled(Level.INFO_INT))
            return;
        if (isEnabledFor(Level.INFO))
            forcedLog(FQCN, Level.INFO, String.format(format, values), null);
    }

    public void info(Throwable t, String format, Object... values) {
        if (repository.isDisabled(Level.INFO_INT))
            return;
        if (isEnabledFor(Level.INFO))
            forcedLog(FQCN, Level.INFO, String.format(format, values), t);
    }

    public void warn(String format, Object... values) {
        if (repository.isDisabled(Level.WARN_INT))
            return;
        if (isEnabledFor(Level.WARN))
            forcedLog(FQCN, Level.WARN, String.format(format, values), null);
    }

    public void warn(Throwable t, String format, Object... values) {
        if (repository.isDisabled(Level.WARN_INT))
            return;
        if (isEnabledFor(Level.INFO))
            forcedLog(FQCN, Level.WARN, String.format(format, values), t);
    }

    public void error(Throwable t, String format, Object... values) {
        if (repository.isDisabled(Level.ERROR_INT))
            return;

        if (isEnabledFor(Level.ERROR))
            forcedLog(FQCN, Level.ERROR, String.format(format, values), t);
    }

    public void error(String format, Object... values) {
        if (repository.isDisabled(Level.ERROR_INT))
            return;

        if (isEnabledFor(Level.ERROR))
            forcedLog(FQCN, Level.ERROR, String.format(format, values), null);
    }

    /**
     * Print an exception with its stack tracke
     *
     * @param l The level
     * @param e The exception
     */
    public void printException(Level l, Throwable e) {
        if (repository.isDisabled(l.getSyslogEquivalent()))
            return;

        if (isEnabledFor(l)) {
            forcedLog(FQCN, Level.ERROR, e.toString(), null);
            LoggerPrintStream out = new LoggerPrintStream(this, l);
            e.printStackTrace(out);
            out.flush();
        }
    }

    public void log(Level level, String format, Object... values) {
        if (repository.isDisabled(level.toInt()))
            return;
        if (isEnabledFor(level))
            forcedLog(FQCN, level, String.format(format, values), null);
    }

    static public interface Factory extends LoggerFactory {
        @Override
        public Logger makeNewLoggerInstance(String name);
    }

    /**
     * Our own logger factory
     */
    static public class DefaultFactory implements Factory {
        @Override
        public Logger makeNewLoggerInstance(String name) {
            return new Logger(name);
        }

    }

}
