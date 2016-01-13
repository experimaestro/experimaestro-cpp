package net.bpiwowar.xpm.utils.log;

/**
 * Our own logger factory
 */
public class DefaultFactory implements Logger.Factory {
    @Override
    public Logger makeNewLoggerInstance(String name) {
        return new Logger(name);
    }

}
