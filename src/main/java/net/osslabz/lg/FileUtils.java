package net.osslabz.lg;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileUtils {

    public static String loadFileContent(File file) throws IOException {
        if (file.getName().endsWith(".gz") || file.getName().endsWith(".tar.gz")) {
            return loadCompressedFile(file);
        } else if (file.getName().endsWith(".zip")) {
            return loadZipFile(file);
        } else {
            return Files.readString(file.toPath());
        }
    }


    public static String loadCompressedFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis);
             GzipCompressorInputStream gzis = new GzipCompressorInputStream(bis);
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzis));
        ) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }


    public static String loadZipFile(File file) throws IOException {
        try (ZipFile zipFile = new ZipFile(file)) {
            for (ZipEntry entry : zipFile.stream().toList()) {
                if (!entry.isDirectory()) {
                    try (InputStream is = zipFile.getInputStream(entry);
                         BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                        return reader.lines().collect(Collectors.joining("\n"));
                    }
                }
            }
            throw new IOException("Couldn't find a file inside zip");
        }
    }
}