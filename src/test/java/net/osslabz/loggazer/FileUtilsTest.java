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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FileUtilsTest {

    private static final String LOG = "2025-01-01 INFO started\n2025-01-01 ERROR failed\n";

    private static final String LOG_WITHOUT_TRAILING_NEWLINE = "2025-01-01 INFO started\n2025-01-01 ERROR failed";

    private static final String UTF8_LOG = "2025-01-01 INFO Grüße aus München";

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
    void decodesGzipFileAsUtf8() throws IOException {
        File file = write("app.log.gz", gzip(UTF8_LOG.getBytes(StandardCharsets.UTF_8)));

        assertEquals(UTF8_LOG, FileUtils.loadFileContent(file));
    }

    @Test
    void decodesLogInZipAsUtf8() throws IOException {
        File file = write("app.zip", zip(Map.of("app.log", UTF8_LOG)));

        assertEquals(UTF8_LOG, FileUtils.loadFileContent(file));
    }

    @Test
    void decodesLogInTarAsUtf8() throws IOException {
        File file = write("app.tar", tar(Map.of("app.log", UTF8_LOG)));

        assertEquals(UTF8_LOG, FileUtils.loadFileContent(file));
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

    @Test
    void loadsEmptyPlainFileAsEmptyText() throws IOException {
        assertEquals("", FileUtils.loadFileContent(write("empty.log", new byte[0])));
    }

    @Test
    void loadsEmptyGzipFileAsEmptyText() throws IOException {
        assertEquals("", FileUtils.loadFileContent(write("empty.log.gz", gzip(new byte[0]))));
    }

    @Test
    void rejectsZipWithoutEntries() throws IOException {
        File file = write("empty.zip", zip(Map.of()));

        assertEquals(
                "No log file found in zip",
                assertThrows(IOException.class, () -> FileUtils.loadFileContent(file))
                        .getMessage());
    }

    @Test
    void rejectsTarWithoutEntries() throws IOException {
        File file = write("empty.tar", tar(Map.of()));

        assertEquals(
                "No log file found in tar",
                assertThrows(IOException.class, () -> FileUtils.loadFileContent(file))
                        .getMessage());
    }

    @Test
    void rejectsZipHoldingOnlyDirectoryEntries() throws IOException {
        File file = write("dirs.zip", zip(Map.of("logs/", "")));

        assertThrows(IOException.class, () -> FileUtils.loadFileContent(file));
    }

    @Test
    void rejectsArchiveWithOnlyIgnoredEntries() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("._app.log", APPLE_DOUBLE_CONTENT);
        entries.put(".DS_Store", DS_STORE_CONTENT);
        entries.put("a/b/deep.log", LOG);
        File zipFile = write("ignored.zip", zip(entries));
        File tarFile = write("ignored.tar", tar(entries));

        assertThrows(IOException.class, () -> FileUtils.loadFileContent(zipFile));
        assertThrows(IOException.class, () -> FileUtils.loadFileContent(tarFile));
    }

    @Test
    void ignoresAppleDoubleEntryInSubdirectoryOfZip() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("logs/._app.log", APPLE_DOUBLE_CONTENT);
        entries.put("logs/app.log", LOG);

        assertEquals(LOG_WITHOUT_TRAILING_NEWLINE, FileUtils.loadFileContent(write("sub.zip", zip(entries))));
    }

    @Test
    void keepsEntryWhoseNameOnlyContainsAppleDoublePrefixMidName() throws IOException {
        File file = write("mid.zip", zip(Map.of("app._v2.log", LOG)));

        assertEquals(LOG_WITHOUT_TRAILING_NEWLINE, FileUtils.loadFileContent(file));
    }

    @Test
    void keepsEntryWhoseNameOnlyEndsLikeDsStore() throws IOException {
        File file = write("lookalike.zip", zip(Map.of("my.DS_Store", LOG)));

        assertEquals(LOG_WITHOUT_TRAILING_NEWLINE, FileUtils.loadFileContent(file));
    }

    @ParameterizedTest
    @ValueSource(strings = {"APP.TAR", "app.TGZ", "app.Tar.Gz", "APP.LOG.GZ", "app.ZIP"})
    void detectsArchiveExtensionsIgnoringCase(String name) throws IOException {
        byte[] content = switch (name.toLowerCase(Locale.ROOT).substring(name.indexOf('.'))) {
            case ".tar" -> tar(Map.of("app.log", LOG));
            case ".tgz", ".tar.gz" -> gzip(tar(Map.of("app.log", LOG)));
            case ".log.gz" -> gzip(LOG.getBytes(StandardCharsets.UTF_8));
            default -> zip(Map.of("app.log", LOG));
        };

        assertEquals(LOG_WITHOUT_TRAILING_NEWLINE, FileUtils.loadFileContent(write(name, content)));
    }

    @Test
    void treatsFileWithArchiveExtensionInTheMiddleAsPlain() throws IOException {
        File file = write("app.gz.txt", LOG.getBytes(StandardCharsets.UTF_8));

        assertEquals(LOG, FileUtils.loadFileContent(file));
    }

    @Test
    void failsOnGzipExtensionWithPlainContent() throws IOException {
        File file = write("fake.gz", LOG.getBytes(StandardCharsets.UTF_8));

        assertThrows(IOException.class, () -> FileUtils.loadFileContent(file));
    }

    @Test
    void failsOnZipExtensionWithPlainContent() throws IOException {
        File file = write("fake.zip", LOG.getBytes(StandardCharsets.UTF_8));

        assertThrows(IOException.class, () -> FileUtils.loadFileContent(file));
    }

    @Test
    void failsOnTruncatedGzipWithIoException() throws IOException {
        byte[] full = gzip(LOG.repeat(1000).getBytes(StandardCharsets.UTF_8));
        File file = write("cut.log.gz", Arrays.copyOf(full, full.length / 2));

        assertThrows(IOException.class, () -> FileUtils.loadFileContent(file));
    }

    @Test
    void failsOnMissingFile() {
        File missing = tempDir.resolve("missing.log").toFile();

        assertThrows(IOException.class, () -> FileUtils.loadFileContent(missing));
    }

    @Test
    void keepsPlainFileCarriageReturnsButNormalizesThemInGzip() throws IOException {
        byte[] crlf = "first\r\nsecond\r\n".getBytes(StandardCharsets.UTF_8);

        assertEquals("first\r\nsecond\r\n", FileUtils.loadFileContent(write("crlf.log", crlf)));
        assertEquals("first\nsecond", FileUtils.loadFileContent(write("crlf.log.gz", gzip(crlf))));
    }

    @Test
    void loadsUnicodeFromEveryWrapping() throws IOException {
        String text = "日本語 \uD83D\uDE00 Grüße ñ";

        assertEquals(text, FileUtils.loadFileContent(write("u.log", text.getBytes(StandardCharsets.UTF_8))));
        assertEquals(text, FileUtils.loadFileContent(write("u.gz", gzip(text.getBytes(StandardCharsets.UTF_8)))));
        assertEquals(text, FileUtils.loadFileContent(write("u.zip", zip(Map.of("u.log", text)))));
        assertEquals(text, FileUtils.loadFileContent(write("u.tar", tar(Map.of("u.log", text)))));
    }

    @Test
    void loadsLargeFileFromEveryWrapping() throws IOException {
        String line = "2025-01-01 INFO " + "x".repeat(200);
        String text = String.join("\n", Collections.nCopies(20_000, line));

        assertEquals(text, FileUtils.loadFileContent(write("big.log", text.getBytes(StandardCharsets.UTF_8))));
        assertEquals(text, FileUtils.loadFileContent(write("big.gz", gzip(text.getBytes(StandardCharsets.UTF_8)))));
        assertEquals(text, FileUtils.loadFileContent(write("big.zip", zip(Map.of("big.log", text)))));
        assertEquals(text, FileUtils.loadFileContent(write("big.tgz", gzip(tar(Map.of("big.log", text))))));
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
    void plainGzipZipAndTarWrappingsOfSameTextLoadToSameLines(long seed) throws IOException {
        Random random = new Random(seed);
        String[] fragments = {"INFO", "ERROR", " ", "\t", "Grüße", "日本語", "\uD83D\uDE00", "{}", "[x]"};
        StringBuilder builder = new StringBuilder();
        int lines = 1 + random.nextInt(50);
        for (int i = 0; i < lines; i++) {
            int count = 1 + random.nextInt(5);
            for (int j = 0; j < count; j++) {
                builder.append(fragments[random.nextInt(fragments.length)]);
            }
            builder.append(i < lines - 1 ? "\n" : "");
        }
        String text = builder.toString();
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        assertEquals(text, FileUtils.loadFileContent(write("p.log", bytes)));
        assertEquals(text, FileUtils.loadFileContent(write("p.log.gz", gzip(bytes))));
        assertEquals(text, FileUtils.loadFileContent(write("p.zip", zip(Map.of("p.log", text)))));
        assertEquals(text, FileUtils.loadFileContent(write("p.tar", tar(Map.of("p.log", text)))));
        assertEquals(text, FileUtils.loadFileContent(write("p.tgz", gzip(tar(Map.of("p.log", text))))));
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
