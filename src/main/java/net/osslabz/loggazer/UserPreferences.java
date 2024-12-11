package net.osslabz.loggazer;

import java.util.prefs.Preferences;

public class UserPreferences {

    private UserPreferences() {
        // intentionally empty
    }

    private static final Preferences PREFS = Preferences.userNodeForPackage(UserPreferences.class);

    public static double getDouble(String key, double defaultValue) {
        return PREFS.getDouble(key, defaultValue);
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        return PREFS.getBoolean(key, defaultValue);
    }

    public static void putDouble(String key, double value) {
        PREFS.putDouble(key, value);
    }

    public static void putBoolean(String key, boolean value) {
        PREFS.putBoolean(key, value);
    }
}