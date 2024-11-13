package dev.skidfuscator.obfuscator.dependency;

import dev.skidfuscator.obfuscator.Skidfuscator;
import lombok.SneakyThrows;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DependencyDownloader {

    @SneakyThrows
    public void download(final CommonDependency dependency) {
        final String url = dependency.getUrl();

        // Resolve path
        Path mappingsDir = Paths.get("mappings-cloud");
        Files.createDirectories(mappingsDir);

        if (Files.exists(mappingsDir.resolve(dependency.name().toLowerCase()))) {
            Skidfuscator.LOGGER.style(String.format("Dependency %s already exists\n", dependency.name()));
            return;
        }

        Path resolvedMappingPath = mappingsDir.resolve(dependency.name().toLowerCase() + "/download.mappings");
        Files.createDirectories(resolvedMappingPath.getParent());

        // Download the zip file
        Skidfuscator.LOGGER.style(String.format("Downloading dependency %s from %s\n", dependency.name(), url));
        try (BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(resolvedMappingPath.toFile())) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        }

        if (!Files.exists(resolvedMappingPath)) {
            throw new IOException("Failed to download the file: " + resolvedMappingPath);
        }

        Skidfuscator.LOGGER.style(String.format("Downloaded dependency %s to %s\n", dependency.name(), resolvedMappingPath));

        if (url.endsWith(".jar")) {
            throw new IllegalArgumentException("Invalid dependency type");
        } else if (url.endsWith(".zip")) {
            try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(resolvedMappingPath))) {
                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    Path filePath = mappingsDir.resolve(entry.getName());
                    if (entry.isDirectory()) {
                        Files.createDirectories(filePath);
                    } else {
                        Files.createDirectories(filePath.getParent());
                        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = zipInputStream.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                    }
                    zipInputStream.closeEntry();
                }
            }
            Skidfuscator.LOGGER.style(String.format("Extracted dependency %s to %s\n", dependency.name(), mappingsDir.toFile().getAbsolutePath()));
        } else if (url.endsWith(".json")) {
            // Copy the file to the mappings directory
            Files.copy(resolvedMappingPath, mappingsDir.resolve(dependency.name().toLowerCase() + ".json"));
            Skidfuscator.LOGGER.style(String.format("Extracted JSON particular dependency %s to %s\n", dependency.name(), mappingsDir.toFile().getAbsolutePath()));
        }
        else {
            Skidfuscator.LOGGER.style(String.format("Unsupported file type for %s\n", url));
        }
        Files.delete(resolvedMappingPath);
    }
}
