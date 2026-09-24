package net.osslabz.loggazer;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javafx.application.Platform;

final class FxTestUtils {

    static final long TIMEOUT_SECONDS = 10;

    private static final Logger JAVAFX_LOGGER = Logger.getLogger("javafx");

    private static boolean toolkitStarted;

    private FxTestUtils() {
        // intentionally empty
    }

    static synchronized void startToolkit() throws InterruptedException {
        if (toolkitStarted) {
            return;
        }
        // surefire puts JavaFX on the class path, which JavaFX reports as an unsupported configuration
        JAVAFX_LOGGER.setFilter(logRecord -> !logRecord.getMessage().startsWith("Unsupported JavaFX configuration"));
        // tests close their windows, which would otherwise shut the toolkit down after the first test
        Platform.setImplicitExit(false);

        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        if (!started.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new IllegalStateException("JavaFX toolkit did not start within " + TIMEOUT_SECONDS + " seconds");
        }
        toolkitStarted = true;
    }

    static <T> T callOnFxThread(Callable<T> action) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.complete(action.call());
            } catch (Throwable t) {
                result.completeExceptionally(t);
            }
        });
        try {
            return result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Exception exception) {
                throw exception;
            }
            if (e.getCause() instanceof Error error) {
                throw error;
            }
            throw e;
        }
    }

    static void runOnFxThread(Runnable action) throws Exception {
        callOnFxThread(() -> {
            action.run();
            return null;
        });
    }
}
