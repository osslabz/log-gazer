package net.osslabz.loggazer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

public class FileUtils {

    private FileUtils() {
        // intentionally empty
    }

    private static boolean isValidLogFile(String path) {
        long slashCount = path.chars().filter(ch -> ch == '/').count();
        return slashCount <= 1 // root level or single subdirectory
                && !isAppleDoubleFile(path)
                && !isDsStoreFile(path);
    }

    // macOS archivers store extended attributes as ._<name> entries next to the real file
    private static boolean isAppleDoubleFile(String path) {
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        return fileName.startsWith("._");
    }

    // Finder writes .DS_Store into every folder it has displayed
    private static boolean isDsStoreFile(String path) {
        return path.equals(".DS_Store") || path.endsWith("/.DS_Store");
    }

    public static String loadFileContent(File file) throws IOException {
        String fileNameLowerCase = file.getName().toLowerCase(Locale.ROOT);
        if (fileNameLowerCase.endsWith(".tar.gz") || fileNameLowerCase.endsWith(".tgz")) {
            return loadFileFromGzipCompressedTarArchive(file);
        } else if (fileNameLowerCase.endsWith(".gz")) {
            return loadGzipCompressedFile(file);
        } else if (fileNameLowerCase.endsWith(".tar")) {
            return loadFileFromTarArchive(file);
        } else if (fileNameLowerCase.endsWith(".zip")) {
            return loadSingleFileFromZipCompressedArchive(file);
        } else {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        }
    }

    public static String loadGzipCompressedFile(File file) throws IOException {
        // Plain .gz files contain a single file by nature
        try (FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);
                GzipCompressorInputStream gzis = new GzipCompressorInputStream(bis)) {
            return readLines(gzis);
        }
    }

    private static String loadFileFromGzipCompressedTarArchive(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);
                GzipCompressorInputStream gzis = new GzipCompressorInputStream(bis)) {
            return loadSingleFileFromTarArchive(gzis);
        }
    }

    private static String loadFileFromTarArchive(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis)) {
            return loadSingleFileFromTarArchive(bis);
        }
    }

    private static String loadSingleFileFromTarArchive(InputStream archive) throws IOException {
        try (TarArchiveInputStream tis = new TarArchiveInputStream(archive)) {

            String content = null;
            String firstName = null;
            TarArchiveEntry entry;

            while ((entry = tis.getNextEntry()) != null) {
                if (!entry.isDirectory() && isValidLogFile(entry.getName())) {
                    if (content != null) {
                        throw new IOException("Tar contains multiple files: " + firstName + ", " + entry.getName());
                    }
                    firstName = entry.getName();
                    content = readLines(tis);
                }
            }

            if (content == null) {
                throw new IOException("No log file found in tar");
            }
            return content;
        }
    }

    public static String loadSingleFileFromZipCompressedArchive(File file) throws IOException {
        try (ZipFile zipFile = new ZipFile(file)) {
            String content = null;
            String firstName = null;

            for (ZipEntry entry : zipFile.stream().toList()) {
                if (!entry.isDirectory() && isValidLogFile(entry.getName())) {
                    if (content != null) {
                        throw new IOException("Zip contains multiple files: " + firstName + ", " + entry.getName());
                    }
                    firstName = entry.getName();
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        content = readLines(is);
                    }
                }
            }

            if (content == null) {
                throw new IOException("No log file found in zip");
            }
            return content;
        }
    }

    // leaves the stream open: a tar stream must stay open to reach its next entry
    private static String readLines(InputStream in) {
        return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
    }
}
