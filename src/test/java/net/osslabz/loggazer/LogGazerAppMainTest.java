package net.osslabz.loggazer;

import java.util.Locale;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LogGazerAppMainTest {

    @Test
    void recognizesUpperCaseVersionOptionInTurkishLocale() {
        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            assertTrue(LogGazerApp.isVersionOption("--VERSION"));
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }
}
