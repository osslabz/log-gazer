package net.osslabz.loggazer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUtilsTest {

    private static final String LOG = "2025-01-01 INFO started\n2025-01-01 ERROR failed\n";

    private static final String LOG_WITHOUT_TRAILING_NEWLINE = "2025-01-01 INFO started\n2025-01-01 ERROR failed";

    @TempDir
    Path tempDir;


    @Test
    void loadsPlainFileVerbatim() throws IOException {
        File file = write("app.log", LOG.getBytes(StandardCharsets.UTF_8));

        assertEquals(LOG, FileUtils.loadFileContent(file));
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
