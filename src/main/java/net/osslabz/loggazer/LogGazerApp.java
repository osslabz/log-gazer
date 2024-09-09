package net.osslabz.loggazer;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;

import java.awt.Taskbar;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class LogGazerApp extends Application {

    private static final String LOG_GAZER = "Log Gazer";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final Map<String, TabContent> tabContent = new HashMap<>();

    private TabPane tabPane;
    private Button buttonMarkLogLevel;
    private Button buttonFormatJson;


    public static void main(String[] args) {
        launch(args);
    }


    @Override
    public void start(Stage primaryStage) {

        if (Taskbar.isTaskbarSupported()) {
            var taskbar = Taskbar.getTaskbar();

            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                final Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
                var dockIcon = defaultToolkit.getImage(getClass().getResource("/icon/icon-256.png"));
                taskbar.setIconImage(dockIcon);
            }
        }

        primaryStage.setTitle(LOG_GAZER);

        WindowUtils.resizeAndPosition(primaryStage);
        primaryStage.setOnCloseRequest((WindowEvent event) -> WindowUtils.saveWindowState(primaryStage));


        Image appIconImage = new Image(LogGazerApp.class.getResourceAsStream("/icon/icon-256.png"));

        primaryStage.getIcons().add(appIconImage);

        this.tabPane = createAndConfigureTabPane(primaryStage);

        BorderPane root = new BorderPane();
        root.setCenter(this.tabPane);

        MenuBar menuBar = createMenuBar();
        ToolBar toolBar = new ToolBar();

        this.buttonMarkLogLevel = createAndCofigureMarkLogLevelButton();
        this.buttonFormatJson = createButtonFormatJson();

        toolBar.getItems().add(buttonFormatJson);
        toolBar.getItems().add(new Separator());
        toolBar.getItems().add(buttonMarkLogLevel);

        VBox topContainer = new VBox(menuBar, toolBar);
        root.setTop(topContainer);

        Scene scene = new Scene(root, 800, 600);

        KeyCombination searchKeyCombination = new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN);
        scene.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (searchKeyCombination.match(event)) {
                Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
                if (selectedTab != null) {
                    showSearchDialog();
                }
            }
        });


        scene.getStylesheets().add(LogGazerApp.class.getResource("/lg.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();
    }


    private void showSearchDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Search");
        dialog.setHeaderText(null);
        dialog.setContentText("Enter search term:");

        dialog.showAndWait().ifPresent(this::searchText);
    }


    private void searchText(String searchTerm) {

        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == null) {
            return;
        }

        TabContent selectedTabContent = this.tabContent.get(selectedTab.getId());

        CodeArea codeArea = selectedTabContent.getCodeArea();
        String text = codeArea.getText().toLowerCase();

        int index = text.indexOf(searchTerm.toLowerCase());
        if (index != -1) {
            codeArea.selectRange(index, index + searchTerm.length());
            codeArea.requestFocus();
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Search Result");
            alert.setHeaderText(null);
            alert.setContentText("Text not found.");
            alert.showAndWait();
        }
    }


    private Button createButtonFormatJson() {
        Button button = new Button("Format JSON");
        button.setDisable(true);

        button.setOnAction(evt -> {
            Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
            if (selectedTab == null) {
                return;
            }

            TabContent selectedTabContent = this.tabContent.get(selectedTab.getId());

            CodeArea codeArea = selectedTabContent.getCodeArea();
            String originalContent = selectedTabContent.getOriginalContent();

            String currentText = codeArea.getText();

            if (currentText.equals(originalContent)) {
                codeArea.replaceText(JsonUtils.format(currentText));
            } else {
                codeArea.replaceText(originalContent);
            }
        });
        return button;
    }


    private Button createAndCofigureMarkLogLevelButton() {
        Button button = new Button("Mark Log Level");
        button.setDisable(true);

        button.setOnAction(evt -> {
            Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
            if (selectedTab == null) {
                return;
            }

            CodeArea codeArea = tabContent.get(selectedTab.getId()).getCodeArea();

            String currentText = codeArea.getText();
            StyleSpans<Collection<String>> currentStyleSpans = codeArea.getStyleSpans(0, currentText.length());

            if (currentStyleSpans.getSpanCount() <= 1) {
                codeArea.setStyleSpans(0, Highlighter.highlightLogLevel(currentText));
            } else {
                codeArea.setStyleSpans(0, Highlighter.computeEmptyStyle(currentText));
            }
        });
        return button;
    }


    private TabPane createAndConfigureTabPane(Stage primaryStage) {
        TabPane tabPane = new TabPane();
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> {

            Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
            if (selectedTab != null) {

                TabContent currentTabContent = this.tabContent.get(selectedTab.getId());
                String currentText = currentTabContent.getCodeArea().getText();

                this.buttonMarkLogLevel.setDisable(false);

                this.buttonFormatJson.setDisable(!JsonUtils.textMightBeJson(currentText));

                File fileInTab = currentTabContent.getFile();
                try {
                    String canonicalPath = fileInTab.getCanonicalPath();
                    primaryStage.setTitle(LOG_GAZER + " - " + canonicalPath);
                } catch (IOException e) {
                    log.warn("Couldn't get canonical path for file {}, falling back to name", fileInTab);
                    primaryStage.setTitle(LOG_GAZER + " - " + nv.getText());
                }
            } else {
                this.buttonFormatJson.setDisable(true);
                this.buttonMarkLogLevel.setDisable(true);
            }
        });
        return tabPane;
    }


    @Override
    public void stop() {
        executor.shutdown();
    }


    private void openFileInNewTab(File file) {
        Task<String> loadTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return FileUtils.loadFileContent(file);
            }
        };

        loadTask.setOnSucceeded(event -> {
            String rawContent = loadTask.getValue();

            CodeArea codeArea = new CodeArea(rawContent);
            codeArea.setEditable(false);
            codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

            TabContent newTabContent = new TabContent(file, rawContent, codeArea);

            VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);

            VBox.setVgrow(scrollPane, Priority.ALWAYS);
            VBox contentBox = new VBox(scrollPane);
            contentBox.setFillWidth(true);

            Tab tab = new Tab(file.getName(), contentBox);
            tab.setId(UUID.randomUUID() + "_" + file.getName());
            this.tabContent.put(tab.getId(), newTabContent);
            this.tabPane.getTabs().add(tab);

            this.tabPane.getSelectionModel().select(tab);
        });

        loadTask.setOnFailed(event -> {
            String message = event.getSource().getException().getMessage();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load file: %s. Error: %s.".formatted(file.getName(), message));
            alert.showAndWait();
        });

        new Thread(loadTask).start();
    }


    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        MenuItem openMenuItem = new MenuItem("Open");
        openMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        openMenuItem.setOnAction(e -> openFile());
        fileMenu.getItems().add(openMenuItem);
        menuBar.getMenus().add(fileMenu);
        return menuBar;
    }


    private void openFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
/*
                new FileChooser.ExtensionFilter("Log Files", "*.log", "*.txt")

                new FileChooser.ExtensionFilter("Compressed Files", "*.gz", "*.zip", "*.tar.gz")
*/
                new FileChooser.ExtensionFilter("Files", "*.*")

        );
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            this.openFileInNewTab(file);
        }
    }
}