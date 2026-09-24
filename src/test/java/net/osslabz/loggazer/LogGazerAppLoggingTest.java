package net.osslabz.loggazer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class LogGazerAppLoggingTest {

    private static final Logger APP_LOGGER = (Logger) LoggerFactory.getLogger("net.osslabz.loggazer");

    private final Level levelBefore = APP_LOGGER.getLevel();

    @AfterEach
    void restoreLevel() {
        APP_LOGGER.setLevel(this.levelBefore);
    }

    @Test
    void disableLoggingSilencesTheAppLogger() {
        APP_LOGGER.setLevel(Level.DEBUG);

        LogGazerApp.disableLogging();

        assertEquals(Level.OFF, APP_LOGGER.getLevel());
    }
}
