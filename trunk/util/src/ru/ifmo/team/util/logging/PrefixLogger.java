package ru.ifmo.team.util.logging;

/**
 * User: Daniel Penkin
 * Date: Apr 27, 2009
 * Version: 1.0
 */
public class PrefixLogger {

    private final String prefix;
    private final Logger logger;

    public PrefixLogger(String prefix, Logger logger) {
        this.prefix = prefix;
        this.logger = logger;
    }

    public void log(String message) {
        if (logger != null) {
            logger.log(prefix + ": " + message);
        }
    }

    public Logger getBaseLogger() {
        return logger;
    }
}
