package net.osslabz.loggazer;

import java.util.prefs.Preferences;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class WindowUtilsTest {

    @Test
    void usesLandscapeDefaultSizeWithoutSavedWindowState() {
        assertInstanceOf(InMemoryPreferencesFactory.InMemoryPreferences.class, Preferences.userNodeForPackage(UserPreferences.class),
                "tests must not use the real user preferences");

        assertEquals(1024.0, WindowUtils.getWidth());
        assertEquals(768.0, WindowUtils.getHeight());
    }
}
