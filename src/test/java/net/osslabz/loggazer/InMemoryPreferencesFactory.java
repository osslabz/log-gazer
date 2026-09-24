package net.osslabz.loggazer;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/**
 * Keeps preferences in memory so tests never read or write the developer's real user preferences.
 */
public class InMemoryPreferencesFactory implements PreferencesFactory {

    private static final Preferences USER_ROOT = new InMemoryPreferences(null, "");

    private static final Preferences SYSTEM_ROOT = new InMemoryPreferences(null, "");

    @Override
    public Preferences systemRoot() {
        return SYSTEM_ROOT;
    }

    @Override
    public Preferences userRoot() {
        return USER_ROOT;
    }

    static class InMemoryPreferences extends AbstractPreferences {

        private final Map<String, String> values = new HashMap<>();

        InMemoryPreferences(InMemoryPreferences parent, String name) {
            super(parent, name);
        }

        @Override
        protected void putSpi(String key, String value) {
            this.values.put(key, value);
        }

        @Override
        protected String getSpi(String key) {
            return this.values.get(key);
        }

        @Override
        protected void removeSpi(String key) {
            this.values.remove(key);
        }

        @Override
        protected void removeNodeSpi() {
            // intentionally empty
        }

        @Override
        protected String[] keysSpi() {
            return this.values.keySet().toArray(new String[0]);
        }

        @Override
        protected String[] childrenNamesSpi() {
            return new String[0];
        }

        @Override
        protected AbstractPreferences childSpi(String name) {
            return new InMemoryPreferences(this, name);
        }

        @Override
        protected void syncSpi() {
            // intentionally empty
        }

        @Override
        protected void flushSpi() {
            // intentionally empty
        }
    }
}
