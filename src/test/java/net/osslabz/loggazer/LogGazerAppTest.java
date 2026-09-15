package net.osslabz.loggazer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static net.osslabz.loggazer.FxTestUtils.callOnFxThread;
import static net.osslabz.loggazer.FxTestUtils.runOnFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LogGazerAppTest {

    @TempDir
    Path tempDir;

    private LogGazerApp app;

    private Stage stage;


    @BeforeAll
    static void startToolkit() throws InterruptedException {
        FxTestUtils.startToolkit();
    }


    @BeforeEach
    void startApp() throws Exception {
        runOnFxThread(() -> {
            this.app = new LogGazerApp();
            this.stage = new Stage();
            this.app.start(this.stage);
        });
    }


    @AfterEach
    void closeApp() throws Exception {
        runOnFxThread(() -> this.stage.close());
    }


    @Test
    void releasesContentOfClosedTab() throws Exception {
        Tab tab = open(write("app.log", "2025-01-01 INFO started\n"));

        runOnFxThread(() -> tabPane().getTabs().remove(tab));

        assertEquals(0, callOnFxThread(() -> this.app.tabContentList.size()));
    }


    private File write(String name, String content) throws IOException {
        return Files.writeString(this.tempDir.resolve(name), content).toFile();
    }


    private Tab open(File file) throws Exception {
        CompletableFuture<Tab> openedTab = new CompletableFuture<>();
        ListChangeListener<Tab> tabAdded = change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    openedTab.complete(change.getAddedSubList().getFirst());
                }
            }
        };
        runOnFxThread(() -> {
            tabPane().getTabs().addListener(tabAdded);
            this.app.openFileInNewTab(file);
        });
        Tab tab = openedTab.get(FxTestUtils.TIMEOUT_SECONDS, TimeUnit.SECONDS);
        runOnFxThread(() -> tabPane().getTabs().removeListener(tabAdded));
        return tab;
    }


    private TabPane tabPane() {
        return (TabPane) ((BorderPane) this.stage.getScene().getRoot()).getCenter();
    }
}
