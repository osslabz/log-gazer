package net.osslabz.loggazer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

final class AppLogging {

    private AppLogging() {
        // intentionally empty
    }

    static void disable() {

        Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.OFF);

        // LogbackConfigurator sets this logger to DEBUG explicitly, which outranks the root level
        Logger appLogger = (Logger) LoggerFactory.getLogger("net.osslabz.loggazer");
        appLogger.setLevel(Level.OFF);
    }
}
