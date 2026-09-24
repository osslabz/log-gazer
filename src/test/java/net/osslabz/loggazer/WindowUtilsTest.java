package net.osslabz.loggazer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.prefs.Preferences;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class WindowUtilsTest {

    @BeforeAll
    static void startToolkit() throws InterruptedException {
        FxTestUtils.startToolkit();
    }

    @Test
    void usesLandscapeDefaultSizeWithoutSavedWindowState() {
        assertInstanceOf(
                InMemoryPreferencesFactory.InMemoryPreferences.class,
                Preferences.userNodeForPackage(UserPreferences.class),
                "tests must not use the real user preferences");

        assertEquals(1024.0, WindowUtils.getWidth());
        assertEquals(768.0, WindowUtils.getHeight());
    }

    @Test
    void leavesMaximizedStateWhenMovingToPrimaryScreen() throws Exception {
        Stage stage = FxTestUtils.callOnFxThread(() -> {
            Stage maximized = new Stage();
            maximized.setMaximized(true);
            WindowUtils.moveToPrimaryScreen(maximized);
            return maximized;
        });

        assertFalse(stage.isMaximized());
    }
}
