package net.osslabz.loggazer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static net.osslabz.loggazer.FxTestUtils.callOnFxThread;
import static net.osslabz.loggazer.FxTestUtils.runOnFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class LogGazerAppTest {

    private static final String JSON_LINES_WITH_TWO_MATCHES =
            "{\"level\":\"INFO\",\"message\":\"first needle\"}\n{\"level\":\"ERROR\",\"message\":\"second needle\"}\n";

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


    @Test
    void nextMatchAfterFormattingJsonSelectsQuery() throws Exception {
        open(write("app.log", JSON_LINES_WITH_TWO_MATCHES));

        runOnFxThread(() -> {
            searchField().setText("needle");
            button("Search").fire();
            button("Format JSON").fire();
            button("Next ▶").fire();
        });

        assertEquals("needle", callOnFxThread(() -> selectedCodeArea().getSelectedText()));
        assertEquals("2 of 2 matches", callOnFxThread(() -> matchCountLabel().getText()));
    }


    @Test
    void nextMatchAfterRestoringOriginalJsonSelectsQuery() throws Exception {
        open(write("app.log", JSON_LINES_WITH_TWO_MATCHES));

        runOnFxThread(() -> {
            button("Format JSON").fire();
            searchField().setText("needle");
            button("Search").fire();
            button("Next ▶").fire();
            button("Format JSON").fire();
            button("Next ▶").fire();
        });

        assertEquals("needle", callOnFxThread(() -> selectedCodeArea().getSelectedText()));
        assertEquals("2 of 2 matches", callOnFxThread(() -> matchCountLabel().getText()));
    }


    @Test
    void formatsJsonFromFileWithCrlfLineEndings() throws Exception {
        open(write("app.log", "{\"level\":\"INFO\",\"message\":\"started\"}\r\n{\"level\":\"ERROR\",\"message\":\"failed\"}\r\n"));

        runOnFxThread(() -> button("Format JSON").fire());

        assertEquals("{\n  \"level\" : \"INFO\",\n  \"message\" : \"started\"\n}\n{\n  \"level\" : \"ERROR\",\n  \"message\" : \"failed\"\n}\n",
                callOnFxThread(() -> selectedCodeArea().getText()));
    }


    @Test
    void reenablesMatchNavigationWhenReturningToSearchedTab() throws Exception {
        Tab searchedTab = open(write("first.log", "2025-01-01 INFO started\n2025-01-01 INFO ready\n"));
        Tab otherTab = open(write("second.log", "2025-01-01 ERROR failed\n"));

        runOnFxThread(() -> {
            tabPane().getSelectionModel().select(searchedTab);
            searchField().setText("INFO");
            button("Search").fire();
            tabPane().getSelectionModel().select(otherTab);
            tabPane().getSelectionModel().select(searchedTab);
        });

        assertFalse(callOnFxThread(() -> button("◀ Previous").isDisabled()));
        assertFalse(callOnFxThread(() -> button("Next ▶").isDisabled()));
    }


    @Test
    void searchButtonDoesNothingBeforeQueryIsEntered() throws Exception {
        open(write("app.log", "2025-01-01 INFO started\n"));

        runOnFxThread(() -> button("Search").fire());

        assertEquals("", callOnFxThread(() -> matchCountLabel().getText()));
        assertNull(callOnFxThread(() -> this.app.tabContentList.values().iterator().next().getSearchData().getQuery()));
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


    private ToolBar toolBar() {
        return (ToolBar) ((VBox) ((BorderPane) this.stage.getScene().getRoot()).getTop()).getChildren().get(1);
    }


    private Button button(String text) {
        return toolBar().getItems().stream()
                .filter(item -> item instanceof Button button && button.getText().equals(text))
                .map(Button.class::cast)
                .findFirst()
                .orElseThrow();
    }


    private TextField searchField() {
        return toolBar().getItems().stream().filter(TextField.class::isInstance).map(TextField.class::cast).findFirst().orElseThrow();
    }


    private Label matchCountLabel() {
        return toolBar().getItems().stream().filter(Label.class::isInstance).map(Label.class::cast).findFirst().orElseThrow();
    }


    private CodeArea selectedCodeArea() {
        return this.app.tabContentList.get(tabPane().getSelectionModel().getSelectedItem().getId()).getCodeArea();
    }
}
