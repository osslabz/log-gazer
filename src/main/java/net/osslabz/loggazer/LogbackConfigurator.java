package net.osslabz.loggazer;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.layout.TTLLLayout;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.spi.ContextAwareBase;

public class LogbackConfigurator extends ContextAwareBase implements Configurator {

    public Configurator.ExecutionStatus configure(LoggerContext loggerContext) {
        addInfo("Setting up default configuration.");

        ConsoleAppender<ILoggingEvent> ca = new ConsoleAppender<>();
        ca.setContext(context);
        ca.setName("console");
        LayoutWrappingEncoder<ILoggingEvent> encoder = new LayoutWrappingEncoder<>();
        encoder.setContext(context);

        TTLLLayout layout = new TTLLLayout();

        layout.setContext(context);
        layout.start();
        encoder.setLayout(layout);

        ca.setEncoder(encoder);
        ca.start();

        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(ca);

        // let the caller decide
        return ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY;
    }
}