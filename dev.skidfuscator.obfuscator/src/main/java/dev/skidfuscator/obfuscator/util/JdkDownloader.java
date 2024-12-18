package dev.skidfuscator.obfuscator.util;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JdkDownloader {
    private static final String OS;
    private static final String JDK_URL;

    static {
        // handle for all os
        OS = System.getProperty("os.name").toLowerCase();

        switch (OS) {
            case "linux":
                JDK_URL = "https://download.java.net/java/GA/jdk17.0.2/0d483333a00540d886896a45e7e18309295e7f3a/jdk-17.0.2_linux-x64_bin.tar.gz";
                break;
            case "mac os x":
            case "mac":
                JDK_URL = "https://corretto.aws/downloads/resources/17.0.13.11.1/amazon-corretto-17.0.13.11.1-macosx-aarch64.tar.gz";
                break;
            case "windows":
            case "windows 11":
                JDK_URL = "https://corretto.aws/downloads/resources/17.0.13.11.1/amazon-corretto-17.0.13.11.1-windows-x64-jdk.zip";
                break;
            default:
                throw new IllegalStateException("Unsupported OS: " + OS);
        }
    }

    private static final Path CACHE_DIR = Paths.get(System.getProperty("user.home"), ".ssvm", "jdk");
    
    public static Path getJdkHome() throws IOException {
        String cacheName;

        switch (OS) {
            case "linux":
                cacheName = "jdk-17.0.2";
                break;
            case "mac os x":
            case "mac":
                cacheName = "amazon-corretto-17.jdk";
                break;
            case "windows":
            case "windows 11":
                cacheName = "jdk17.0.13_11";
                break;
            default:
                throw new IllegalStateException("Unsupported OS: " + OS);
        }

        Path jdkPath = CACHE_DIR.resolve(cacheName);
        if (Files.exists(jdkPath)) {
            System.out.println("JDK 17 already downloaded to " + jdkPath);
            switch (OS) {
                case "mac os x":
                case "mac":
                    return jdkPath.resolve("Contents/Home");
            }
            return jdkPath;
        }

        System.out.println("JDK 17 not found in cache at " + jdkPath);
        
        Files.createDirectories(CACHE_DIR);
        System.out.println("Downloading JDK 17...");

        if (JDK_URL.endsWith(".zip")) {
            try (ZipInputStream zis = new ZipInputStream(new URL(JDK_URL).openStream())) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path destPath = CACHE_DIR.resolve(entry.getName());
                    if (entry.isDirectory()) {
                        Files.createDirectories(destPath);
                    } else {
                        Files.createDirectories(destPath.getParent());
                        Files.copy(zis, destPath);
                    }
                }
            } catch (IOException e) {
                Files.deleteIfExists(jdkPath);
                throw e;
            }
        } else {
            try (TarArchiveInputStream tis = new TarArchiveInputStream(
                    new GzipCompressorInputStream(
                        new URL(JDK_URL).openStream()
                    ))) {
                TarArchiveEntry entry;
                while ((entry = tis.getNextTarEntry()) != null) {
                    Path destPath = CACHE_DIR.resolve(entry.getName());
                    if (entry.isDirectory()) {
                        Files.createDirectories(destPath);
                    } else {
                        Files.createDirectories(destPath.getParent());
                        Files.copy(tis, destPath);
                    }
                }
            } catch (IOException e) {
                Files.deleteIfExists(jdkPath);
                throw e;
            }
        }

        System.out.println("JDK 17 downloaded to " + jdkPath);

        switch (OS) {
            case "mac os x":
            case "mac":
                return jdkPath.resolve("Contents/Home");
        }
        return jdkPath;
    }
    
    public static String getJmodPath() throws IOException {
        return getJdkHome().resolve("jmods").toString();
    }
}