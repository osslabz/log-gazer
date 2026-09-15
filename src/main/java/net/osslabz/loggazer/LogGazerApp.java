package net.osslabz.loggazer;

import ch.qos.logback.classic.Level;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.commons.lang3.StringUtils;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LogGazerApp extends Application {

    public static final Logger log = LoggerFactory.getLogger(LogGazerApp.class);

    private static final String LOG_GAZER = "Log Gazer";

    public static final String SEARCH_QUERY_PLACEHOLDER = "Search...";

    private static boolean loggingDisabled = true;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final Map<String, TabContent> tabContentList = new HashMap<>();

    private TabPane tabPane;

    private Button buttonMarkLogLevel;

    private Button buttonFormatJson;

    private TextField searchField;

    private Button searchButton;

    private Button prevMatchButton;

    private Button nextMatchButton;

    private Label matchCountLabel;


    public static void main(String[] args) {

        if (loggingDisabled) {
            disableLogging();
        }

        if (args != null && args.length == 1) {
            String singleParam = args[0];
            if (singleParam != null) {
                String paramLowerCase = StringUtils.stripStart(singleParam.trim().toLowerCase(), "-");
                if (paramLowerCase.equals("version") || paramLowerCase.equals("v")) {
                    System.out.printf(
                        """
                            log-gazer %s%n
                            Copyright (C) 2024 Raphael Vullriede (raphael@osslabz.net)%n
                            License: Apache License Version 2.0, January 2004 <https://www.apache.org/licenses/LICENSE-2.0.txt>.%n
                            This is free software: you are free to change and redistribute it.%n
                            There is NO WARRANTY, to the extent permitted by law.%n
                            """, LogGazerApp.class.getPackage().getImplementationVersion());
                    System.exit(0);
                }
            }
        }

        launch(LogGazerApp.class, args);
    }


    @Override
    public void start(Stage primaryStage) {

        if (Taskbar.isTaskbarSupported()) {
            log.debug("Taskbar is supported");
            var taskbar = Taskbar.getTaskbar();

            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                log.debug("Taskbar.Feature.ICON_IMAGE is supported");
                final Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
                var dockIcon = defaultToolkit.getImage(getClass().getResource("/icon/icon-256.png"));
                taskbar.setIconImage(dockIcon);
            } else {
                log.debug("Taskbar.Feature.ICON_IMAGE is NOT supported");
            }
        } else {
            log.debug("Taskbar is NOT supported");
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
        ToolBar toolBar = createToolBar();

        VBox topContainer = new VBox(menuBar, toolBar);
        root.setTop(topContainer);

        Scene scene = new Scene(root, 800, 600);

        KeyCombination searchKeyCombination = new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN);
        scene.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (searchKeyCombination.match(event)) {
                Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
                if (selectedTab != null) {
                    this.searchField.requestFocus();
                }
            }
        });

        scene.getStylesheets().add(LogGazerApp.class.getResource("/lg.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();

        // getParameters().getUnnamed()
    }


    private ToolBar createToolBar() {

        ToolBar toolBar = new ToolBar();

        this.buttonMarkLogLevel = createAndCofigureMarkLogLevelButton();
        this.buttonFormatJson = createButtonFormatJson();

        this.searchField = new TextField();
        searchField.setMinWidth(20);
        searchField.setOnAction(e -> performSearch());
        searchField.setOnMouseClicked(event -> {
            if (SEARCH_QUERY_PLACEHOLDER.equals(searchField.getText())) {
                searchField.setText("");
            }
        });
        searchField.setDisable(true);

        this.searchButton = new Button("Search");
        searchButton.setOnAction(e -> performSearch());
        searchButton.setDisable(true);

        this.prevMatchButton = new Button("◀ Previous");
        prevMatchButton.setOnAction(e -> navigateToPreviousMatch());
        prevMatchButton.setDisable(true);

        this.nextMatchButton = new Button("Next ▶");
        nextMatchButton.setOnAction(e -> navigateToNextMatch());
        nextMatchButton.setDisable(true);

        this.matchCountLabel = new Label();

        resetSearch();

        toolBar.getItems().addAll(
            this.buttonFormatJson,
            new Separator(),
            this.buttonMarkLogLevel,
            new Separator(),
            this.searchField,
            this.searchButton,
            this.prevMatchButton,
            this.nextMatchButton,
            this.matchCountLabel

        );

        return toolBar;
    }


    private void performSearch() {

        TabContent currentTabContent = this.getCurrentTabContent();
        if (currentTabContent == null) {
            return;
        }

        CodeArea codeArea = currentTabContent.getCodeArea();

        String searchTerm = searchField.getText();
        if (searchTerm.isEmpty()) {
            return;
        }

        List<Integer> matchPositions = SearchUtils.findAllMatches(codeArea, searchTerm);

        currentTabContent.updateSearch(searchTerm, matchPositions);

        if (matchPositions.isEmpty()) {
            codeArea.selectRange(codeArea.getAnchor(), codeArea.getAnchor());
            matchCountLabel.setText("No matches found");
            prevMatchButton.setDisable(true);
            nextMatchButton.setDisable(true);
            return;
        }

        navigateToCurrentMatch();

        // Enable/disable buttons based on match count
        prevMatchButton.setDisable(matchPositions.size() <= 1);
        nextMatchButton.setDisable(matchPositions.size() <= 1);
    }


    private void navigateToCurrentMatch() {

        TabContent tabContent = this.getCurrentTabContent();
        if (tabContent == null || tabContent.getSearchData().getCurrentMatchIndex() == -1) {
            resetSearch();
            return;
        }

        int currentMatchIndex = tabContent.getSearchData().getCurrentMatchIndex();
        int numMatches = tabContent.getSearchData().numMatches();

        searchField.setText(StringUtils.isNotBlank(tabContent.getSearchData().getQuery()) ? tabContent.getSearchData().getQuery() :
            SEARCH_QUERY_PLACEHOLDER);
        matchCountLabel.setText(String.format("%d of %d matches", currentMatchIndex + 1, numMatches));

        if (currentMatchIndex >= 0 && currentMatchIndex < numMatches) {
            int position = tabContent.getSearchData().getCurrentMatchPosition();
            CodeArea codeArea = tabContent.getCodeArea();
            codeArea.moveTo(position);
            codeArea.selectRange(position, position + tabContent.getSearchData().getQuery().length());
            codeArea.requestFollowCaret();
            codeArea.requestFocus();
        }
    }


    private void resetSearch() {

        searchField.setText(SEARCH_QUERY_PLACEHOLDER);
        matchCountLabel.setText("");
    }


    private void navigateToNextMatch() {

        TabContent currentTabContent = this.getCurrentTabContent();
        if (currentTabContent == null || currentTabContent.getSearchData().getCurrentMatchIndex() == -1) {
            return;
        }
        currentTabContent.getSearchData().moveToNextMatch();
        navigateToCurrentMatch();
    }


    private void navigateToPreviousMatch() {

        TabContent currentTabContent = this.getCurrentTabContent();
        if (currentTabContent == null || currentTabContent.getSearchData().getCurrentMatchIndex() == -1) {
            return;
        }
        currentTabContent.getSearchData().moveToPrevMatch();
        navigateToCurrentMatch();
    }


    private TabContent getCurrentTabContent() {

        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == null) {
            return null;
        }

        return this.tabContentList.get(selectedTab.getId());
    }


    private Button createButtonFormatJson() {

        Button button = new Button("Format JSON");
        button.setDisable(true);

        button.setOnAction(evt -> {
            Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
            if (selectedTab == null) {
                return;
            }

            TabContent selectedTabContent = this.tabContentList.get(selectedTab.getId());

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

            CodeArea codeArea = tabContentList.get(selectedTab.getId()).getCodeArea();

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

                TabContent currentTabContent = this.tabContentList.get(selectedTab.getId());
                String currentText = currentTabContent.getCodeArea().getText();

                enableButtons(true);
                navigateToCurrentMatch();

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
                enableButtons(false);
                resetSearch();
            }
        });
        return tabPane;
    }


    private void enableButtons(boolean enable) {

        this.buttonMarkLogLevel.setDisable(!enable);
        this.buttonFormatJson.setDisable(!enable);
        this.searchField.setDisable(!enable);
        this.searchButton.setDisable(!enable);
        this.prevMatchButton.setDisable(!enable);
        this.nextMatchButton.setDisable(!enable);
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

            scrollPane.addEventFilter(ScrollEvent.SCROLL, scrollEvent -> {
                log.trace("scrollEvent={}", scrollEvent);
                scrollPane.scrollYBy(scrollEvent.getDeltaY() * -1);
                //   scrollPane.scrollXBy(scrollEvent.getDeltaX());

                scrollEvent.consume();
            });

            Tab tab = new Tab(file.getName(), contentBox);

            tab.setId(UUID.randomUUID() + "_" + file.getName());
            this.tabContentList.put(tab.getId(), newTabContent);
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


    public static void enableLogging() {

        loggingDisabled = false;
    }


    private static void disableLogging() {

        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.OFF);
    }
}