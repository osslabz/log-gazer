package net.osslabz.loggazer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.spi.ConfiguratorRank;
import ch.qos.logback.core.spi.ContextAwareBase;

/**
 * Turns logging off in tests; it outranks {@link LogbackConfigurator}, which logs the app at DEBUG.
 */
@ConfiguratorRank(ConfiguratorRank.CUSTOM_HIGH_PRIORITY)
public class SilentLogbackConfigurator extends ContextAwareBase implements Configurator {

    @Override
    public ExecutionStatus configure(LoggerContext loggerContext) {
        loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.OFF);
        return ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY;
    }
}
