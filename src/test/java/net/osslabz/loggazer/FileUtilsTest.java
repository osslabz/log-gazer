package net.osslabz.loggazer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileUtilsTest {

    private static final String LOG = "2025-01-01 INFO started\n2025-01-01 ERROR failed\n";

    private static final String LOG_WITHOUT_TRAILING_NEWLINE = "2025-01-01 INFO started\n2025-01-01 ERROR failed";

    private static final String APPLE_DOUBLE_CONTENT = "Mac OS X extended attributes";

    private static final String DS_STORE_CONTENT = "Bud1";

    @TempDir
    Path tempDir;

    @Test
    void loadsPlainFileVerbatim() throws IOException {
        File file = write("app.log", LOG.getBytes(StandardCharsets.UTF_8));

        assertEquals(LOG, FileUtils.loadFileContent(file));
    }

    @Test
    void replacesInvalidUtf8BytesInPlainFile() throws IOException {
        File file = write("latin1.log", "2025-01-01 INFO Grüße aus München\n".getBytes(StandardCharsets.ISO_8859_1));

        assertEquals("2025-01-01 INFO Gr\uFFFD\uFFFDe aus M\uFFFDnchen\n", FileUtils.loadFileContent(file));
    }

    @Test
    void loadsGzipFileAsJoinedLines() throws IOException {
        File file = write("app.log.gz", gzip(LOG.getBytes(StandardCharsets.UTF_8)));

        assertEquals(LOG_WITHOUT_TRAILING_NEWLINE, FileUtils.loadFileContent(file));
    }

    @Test
    void detectsGzipExtensionIgnoringCase() throws IOException {
        File file = write("APP.LOG.GZ", gzip(LOG.getBytes(StandardCharsets.UTF_8)));

        assertEquals(LOG_WITHOUT_TRAILING_NEWLINE, FileUtils.loadFileContent(file));
    }

    @Test
    void detectsZipExtensionIgnoringCaseInTurkishLocale() throws IOException {
        File file = write("LOGS.ZIP", zip(Map.of("app.log", LOG)));

        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            assertEquals(LOG_WITHOUT_TRAILING_NEWLINE, FileUtils.loadFileContent(file));
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

    @Test
    void loadsSingleLogFromZip() throws IOException {
        File file = write("app.zip", zip(Map.of("app.log", LOG)));

        assertEquals(LOG_WITHOUT_TRAILING_NEWLINE, FileUtils.loadFileContent(file));
    }

    @Test
    void loadsSingleLogFromOneDirectoryDeepInZip() throws IOException {
        File file = write("app.zip", zip(Map.of("logs/app.log", LOG)));

        assertEquals(LOG_WITHOUT_TRAILING_NEWLINE, FileUtils.loadFileContent(file));
    }

    @Test
    void ignoresLogsNestedDeeperThanOneDirectoryInZip() throws IOException {
        File file = write("app.zip", zip(Map.of("var/logs/app.log", LOG)));

        IOException e = assertThrows(IOException.class, () -> FileUtils.loadFileContent(file));
        assertEquals("No log file found in zip", e.getMessage());
    }

    @Test
    void rejectsZipWithMoreThanOneLog() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("app.log", LOG);
        entries.put("other.log", LOG);
        File file = write("app.zip", zip(entries));

        IOException e = assertThrows(IOException.class, () -> FileUtils.loadFileContent(file));
        assertTrue(e.getMessage().contains("app.log") && e.getMessage().contains("other.log"), e.getMessage());
    }

    @Test
    void ignoresAppleDoubleEntriesInZip() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("app.log", LOG);
        entries.put("__MACOSX/", "");
        entries.put("__MACOSX/._app.log", APPLE_DOUBLE_CONTENT);
        File file = write("app.log.zip", zip(entries));

        assertEquals(LOG_WITHOUT_TRAILING_NEWLINE, FileUtils.loadFileContent(file));
    }

    @Test
    void ignoresAppleDoubleEntriesInTarGz() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("._app.log", APPLE_DOUBLE_CONTENT);
        entries.put("app.log", LOG);
        File file = write("app.tar.gz", gzip(tar(entries)));

        assertEquals(LOG_WITHOUT_TRAILING_NEWLINE, FileUtils.loadFileContent(file));
    }

    @Test
    void ignoresDsStoreEntriesInZip() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("logs/.DS_Store", DS_STORE_CONTENT);
        entries.put("logs/app.log", LOG);
        File file = write("logs.zip", zip(entries));

        assertEquals(LOG_WITHOUT_TRAILING_NEWLINE, FileUtils.loadFileContent(file));
    }

    @Test
    void ignoresDsStoreEntriesInTar() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put(".DS_Store", DS_STORE_CONTENT);
        entries.put("app.log", LOG);
        File file = write("logs.tar", tar(entries));

        assertEquals(LOG_WITHOUT_TRAILING_NEWLINE, FileUtils.loadFileContent(file));
    }

    @Test
    void loadsSingleLogFromTarGz() throws IOException {
        File file = write("app.tar.gz", gzip(tar(Map.of("app.log", LOG))));

        assertEquals(LOG_WITHOUT_TRAILING_NEWLINE, FileUtils.loadFileContent(file));
    }

    @Test
    void loadsSingleLogFromTgz() throws IOException {
        File file = write("app.tgz", gzip(tar(Map.of("app.log", LOG))));

        assertEquals(LOG_WITHOUT_TRAILING_NEWLINE, FileUtils.loadFileContent(file));
    }

    @Test
    void loadsSingleLogFromTar() throws IOException {
        File file = write("app.tar", tar(Map.of("app.log", LOG)));

        assertEquals(LOG_WITHOUT_TRAILING_NEWLINE, FileUtils.loadFileContent(file));
    }

    @Test
    void ignoresLogsNestedDeeperThanOneDirectoryInTar() throws IOException {
        File file = write("app.tar", tar(Map.of("var/logs/app.log", LOG)));

        IOException e = assertThrows(IOException.class, () -> FileUtils.loadFileContent(file));
        assertEquals("No log file found in tar", e.getMessage());
    }

    @Test
    void rejectsTarWithMoreThanOneLog() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("app.log", LOG);
        entries.put("other.log", LOG);
        File file = write("app.tar", tar(entries));

        IOException e = assertThrows(IOException.class, () -> FileUtils.loadFileContent(file));
        assertTrue(e.getMessage().contains("app.log") && e.getMessage().contains("other.log"), e.getMessage());
    }

    private File write(String name, byte[] content) throws IOException {
        return Files.write(tempDir.resolve(name), content).toFile();
    }

    private static byte[] gzip(byte[] content) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (OutputStream out = new GZIPOutputStream(bytes)) {
            out.write(content);
        }
        return bytes.toByteArray();
    }

    private static byte[] tar(Map<String, String> entries) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(bytes)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                byte[] content = entry.getValue().getBytes(StandardCharsets.UTF_8);
                TarArchiveEntry tarEntry = new TarArchiveEntry(entry.getKey());
                tarEntry.setSize(content.length);
                out.putArchiveEntry(tarEntry);
                out.write(content);
                out.closeArchiveEntry();
            }
        }
        return bytes.toByteArray();
    }

    private static byte[] zip(Map<String, String> entries) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                out.putNextEntry(new ZipEntry(entry.getKey()));
                out.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        return bytes.toByteArray();
    }
}
