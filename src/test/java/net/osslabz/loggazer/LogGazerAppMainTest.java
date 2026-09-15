package net.osslabz.loggazer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogGazerAppMainTest {

    @TempDir
    Path tempDir;

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


    @Test
    void printsVersionWithoutBlankLines() throws Exception {
        // the java launcher refuses a main class that extends Application when JavaFX is on the class path
        Path outputFile = tempDir.resolve("version.out");
        Process process = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", System.getProperty("java.class.path"),
                AppStarter.class.getName(), "--version")
                .redirectErrorStream(true)
                .redirectOutput(outputFile.toFile())
                .start();

        boolean exited;
        try {
            exited = process.waitFor(30, TimeUnit.SECONDS);
        } finally {
            process.destroyForcibly();
        }

        assertTrue(exited, "java --version did not exit");
        assertEquals("""
                log-gazer null
                Copyright (C) 2024 Raphael Vullriede (raphael@osslabz.net)
                License: Apache License Version 2.0, January 2004 <https://www.apache.org/licenses/LICENSE-2.0.txt>.
                This is free software: you are free to change and redistribute it.
                There is NO WARRANTY, to the extent permitted by law.
                """, Files.readString(outputFile));
        assertEquals(0, process.exitValue());
    }
}
