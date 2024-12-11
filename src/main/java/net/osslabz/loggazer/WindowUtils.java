package net.osslabz.loggazer;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class WindowUtils {

    private static final String PREFS_X = "windowX";
    private static final String PREFS_Y = "windowY";
    private static final String PREFS_WIDTH = "windowWidth";
    private static final String PREFS_HEIGHT = "windowHeight";
    private static final String PREFS_MAXIMIZED = "windowMaximized";

    private static final double MINIMUM_VISIBLE_WIDTH = 100;
    private static final double MINIMUM_VISIBLE_HEIGHT = 50;
    private static final double MARGIN = 50;
    private static final double DEFAULT_WIDTH = 1024;
    private static final double DEFAULT_HEIGHT = 768;


    private WindowUtils() {
        // intentionally empty
    }


    protected static void resizeAndPosition(Stage stage) {
        Platform.runLater(() -> {

            // We'll leave the initial position to the OS
            if (getX() != -1) {
                stage.setX(getX());
            }
            if (getY() != -1) {
                stage.setY(getY());
            }

            stage.setWidth(getWidth());
            stage.setHeight(getHeight());
            stage.setMaximized(getMaximized());

            new Thread(() -> {
                if (WindowUtils.isWindowIsOutOfBounds(stage)) {
                    WindowUtils.moveToPrimaryScreen(stage);
                }
            }).start();
        });
    }

    private static boolean getMaximized() {
        return UserPreferences.getBoolean(PREFS_MAXIMIZED, false);
    }

    private static double getHeight() {
        return UserPreferences.getDouble(PREFS_HEIGHT, DEFAULT_WIDTH);
    }

    private static double getWidth() {
        return UserPreferences.getDouble(PREFS_WIDTH, DEFAULT_HEIGHT);
    }

    private static double getY() {
        return UserPreferences.getDouble(PREFS_Y, -1);
    }

    private static double getX() {
        return UserPreferences.getDouble(PREFS_X, -1);
    }


    protected static boolean isWindowIsOutOfBounds(Stage stage) {
        for (Screen screen : Screen.getScreens()) {
            Rectangle2D bounds = screen.getVisualBounds();
            if (stage.getX() + stage.getWidth() - MINIMUM_VISIBLE_WIDTH >= bounds.getMinX() &&
                    stage.getX() + MINIMUM_VISIBLE_WIDTH <= bounds.getMaxX() &&
                    bounds.getMinY() <= stage.getY() && // We want the title bar to always be visible.
                    stage.getY() + MINIMUM_VISIBLE_HEIGHT < bounds.getMaxY()) {
                return false;
            }
        }
        return true;
    }


    protected static void moveToPrimaryScreen(Stage stage) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getMinX() + MARGIN);
        stage.setY(bounds.getMinY() + MARGIN);
        stage.setWidth(DEFAULT_WIDTH);
        stage.setHeight(DEFAULT_HEIGHT);
    }


    protected static void saveWindowState(Stage stage) {
        UserPreferences.putDouble(PREFS_X, stage.getX());
        UserPreferences.putDouble(PREFS_Y, stage.getY());
        UserPreferences.putDouble(PREFS_WIDTH, stage.getWidth());
        UserPreferences.putDouble(PREFS_HEIGHT, stage.getHeight());
        UserPreferences.putBoolean(PREFS_MAXIMIZED, stage.isMaximized());
    }
}