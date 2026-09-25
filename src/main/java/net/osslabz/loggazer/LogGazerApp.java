package net.osslabz.loggazer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javafx.application.Application;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.commons.lang3.StringUtils;
import org.fxmisc.richtext.CodeArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogGazerApp extends Application {

    public static final Logger log = LoggerFactory.getLogger(LogGazerApp.class);

    private static final String LOG_GAZER = "Log Gazer";

    public static final String SEARCH_QUERY_PLACEHOLDER = "Search...";

    private static boolean loggingDisabled = true;

    final Map<String, TabContent> tabContentList = new HashMap<>();

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
            AppLogging.disable();
        }

        if (args != null && args.length == 1 && isVersionOption(args[0])) {
            System.out.print("log-gazer " + LogGazerApp.class.getPackage().getImplementationVersion() + "\n" + """
                            Copyright (C) 2024 Raphael Vullriede (raphael@osslabz.net)
                            License: Apache License Version 2.0, January 2004 <https://www.apache.org/licenses/LICENSE-2.0.txt>.
                            This is free software: you are free to change and redistribute it.
                            There is NO WARRANTY, to the extent permitted by law.
                            """);
            System.exit(0);
        }

        launch(LogGazerApp.class, args);
    }

    static boolean isVersionOption(String param) {

        if (param == null) {
            return false;
        }
        String paramLowerCase = StringUtils.stripStart(param.trim().toLowerCase(Locale.ROOT), "-");
        return "version".equals(paramLowerCase) || "v".equals(paramLowerCase);
    }

    @Override
    public void start(Stage primaryStage) {

        AppIcon.setTaskbarIcon();

        primaryStage.setTitle(LOG_GAZER);

        WindowUtils.resizeAndPosition(primaryStage);
        primaryStage.setOnCloseRequest((WindowEvent event) -> WindowUtils.saveWindowState(primaryStage));

        AppIcon.addTo(primaryStage);

        this.tabPane = createAndConfigureTabPane(primaryStage);

        BorderPane root = new BorderPane();
        root.setCenter(this.tabPane);

        MenuBar menuBar = FileMenu.createMenuBar(this::openFileInNewTab);
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
        searchField.setPromptText(SEARCH_QUERY_PLACEHOLDER);
        searchField.setOnAction(e -> performSearch());
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

        toolBar.getItems()
                .addAll(
                        this.buttonFormatJson,
                        new Separator(),
                        this.buttonMarkLogLevel,
                        new Separator(),
                        this.searchField,
                        this.searchButton,
                        this.prevMatchButton,
                        this.nextMatchButton,
                        this.matchCountLabel);

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
            currentTabContent.updateSearch(searchTerm, List.of());
            matchCountLabel.setText("");
            toggleSearchButtons(false);
            return;
        }

        List<Integer> matchPositions = SearchUtils.findAllMatches(codeArea, searchTerm);

        currentTabContent.updateSearch(searchTerm, matchPositions);

        if (matchPositions.isEmpty()) {
            codeArea.selectRange(codeArea.getAnchor(), codeArea.getAnchor());
            matchCountLabel.setText("No matches found");
            toggleSearchButtons(false);
            return;
        }

        toggleSearchButtons(true);
        navigateToCurrentMatch();
    }

    private void navigateToCurrentMatch() {

        TabContent tabContent = this.getCurrentTabContent();
        if (tabContent == null || tabContent.getSearchData().getCurrentMatchIndex() == -1) {
            resetSearch();
            return;
        }

        int currentMatchIndex = tabContent.getSearchData().getCurrentMatchIndex();
        int numMatches = tabContent.getSearchData().numMatches();

        searchField.setText(tabContent.getSearchData().getQuery());
        matchCountLabel.setText(String.format("%d of %d matches", currentMatchIndex + 1, numMatches));

        if (currentMatchIndex >= 0 && currentMatchIndex < numMatches) {
            toggleSearchButtons(true);
            int position = tabContent.getSearchData().getCurrentMatchPosition();
            CodeArea codeArea = tabContent.getCodeArea();
            codeArea.moveTo(position);
            codeArea.selectRange(
                    position, position + tabContent.getSearchData().getQuery().length());
            codeArea.requestFollowCaret();
            codeArea.requestFocus();
        }
    }

    private void resetSearch() {

        searchField.clear();
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

            // replaceText drops the styling, so the toggle must forget it was marked
            selectedTabContent.setLogLevelMarked(false);

            // match positions refer to the replaced text
            String query = selectedTabContent.getSearchData().getQuery();
            if (query != null) {
                this.searchField.setText(query);
                performSearch();
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

            TabContent tabContent = tabContentList.get(selectedTab.getId());
            CodeArea codeArea = tabContent.getCodeArea();

            String currentText = codeArea.getText();

            if (tabContent.isLogLevelMarked()) {
                codeArea.setStyleSpans(0, Highlighter.computeEmptyStyle(currentText));
            } else {
                codeArea.setStyleSpans(0, Highlighter.highlightLogLevel(currentText));
            }
            tabContent.setLogLevelMarked(!tabContent.isLogLevelMarked());
        });
        return button;
    }

    private TabPane createAndConfigureTabPane(Stage primaryStage) {

        TabPane tabPane = new TabPane();
        // the close button removes the tab from this list too, while removing it in code fires no onClosed
        // a reorder removes and re-adds the same tab, so only drop content for a tab that is gone for good
        tabPane.getTabs().addListener((ListChangeListener<Tab>) change -> {
            while (change.next()) {
                change.getRemoved().stream()
                        .filter(tab -> !change.getList().contains(tab))
                        .forEach(tab -> this.tabContentList.remove(tab.getId()));
            }
        });
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
                primaryStage.setTitle(LOG_GAZER);
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
        this.toggleSearchButtons(false);
    }

    private void toggleSearchButtons(boolean enable) {
        this.prevMatchButton.setDisable(!enable);
        this.nextMatchButton.setDisable(!enable);
    }

    void openFileInNewTab(File file) {

        Task<String> loadTask = new Task<>() {
            @Override
            protected String call() throws Exception {

                return FileUtils.loadFileContent(file);
            }
        };

        loadTask.setOnSucceeded(event -> {
            String rawContent = loadTask.getValue();

            TabContent newTabContent = LogTabFactory.createContent(file, rawContent);
            Tab tab = LogTabFactory.createTab(newTabContent);

            this.tabContentList.put(tab.getId(), newTabContent);
            this.tabPane.getTabs().add(tab);

            this.tabPane.getSelectionModel().select(tab);
        });

        loadTask.setOnFailed(event -> {
            String message = event.getSource().getException().getMessage();
            Alert alert = new Alert(
                    Alert.AlertType.ERROR, "Failed to load file: %s. Error: %s.".formatted(file.getName(), message));
            alert.showAndWait();
        });

        new Thread(loadTask).start();
    }

    public static void enableLogging() {

        loggingDisabled = false;
    }
}
