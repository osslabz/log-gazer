package net.osslabz.loggazer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javafx.application.Application;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogGazerApp extends Application {

    public static final Logger log = LoggerFactory.getLogger(LogGazerApp.class);

    private static final String LOG_GAZER = "Log Gazer";

    private static boolean loggingDisabled = true;

    final Map<String, TabContent> tabContentList = new HashMap<>();

    private TabPane tabPane;

    private LogToolBar toolBar;

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
        this.toolBar = new LogToolBar(this::getCurrentTabContent);

        VBox topContainer = new VBox(menuBar, this.toolBar);
        root.setTop(topContainer);

        Scene scene = new Scene(root, 800, 600);

        this.toolBar.focusSearchOnShortcut(scene);

        scene.getStylesheets().add(LogGazerApp.class.getResource("/lg.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();

        // getParameters().getUnnamed()
    }

    private TabContent getCurrentTabContent() {

        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == null) {
            return null;
        }

        return this.tabContentList.get(selectedTab.getId());
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
                this.toolBar.showTab(currentTabContent);

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
                this.toolBar.showNoTab();
            }
        });
        return tabPane;
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
