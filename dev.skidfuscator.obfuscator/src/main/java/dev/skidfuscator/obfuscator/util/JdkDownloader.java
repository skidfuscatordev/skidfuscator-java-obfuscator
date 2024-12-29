package dev.skidfuscator.obfuscator.util;

import dev.skidfuscator.obfuscator.util.progress.ProgressWrapper;
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
    private static boolean jdkDownloaded = false;

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
            case "windows 10":
                JDK_URL = "https://corretto.aws/downloads/resources/17.0.13.11.1/amazon-corretto-17.0.13.11.1-windows-x64-jdk.zip";
                break;
            default:
                throw new IllegalStateException("Unsupported OS: " + OS);
        }
    }

    public static final Path CACHE_DIR = Paths.get(System.getProperty("user.home"), ".ssvm", "jdk");

    public static Path getCachedJdk() throws IOException {
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
            case "windows 10":
            case "windows 11":
                cacheName = "jdk17.0.13_11";
                break;
            default:
                throw new IllegalStateException("Unsupported OS: " + OS);
        }

        Path jdkPath = CACHE_DIR.resolve(cacheName);
        if (Files.exists(jdkPath)) {
            jdkDownloaded = true;
            //System.out.println("JDK 17 already downloaded to " + jdkPath);
            switch (OS) {
                case "mac os x":
                case "mac":
                    return jdkPath.resolve("Contents/Home");
            }
            return jdkPath;
        }

        return null;
    }

    public static Path getJdkHome() throws IOException {
        jdkDownloaded = true;
        Path jdkPath = getCachedJdk();

        if (jdkPath != null) {
            return jdkPath;
        }

        System.out.println("JDK 17 not found in cache at " + jdkPath);
        
        Files.createDirectories(CACHE_DIR);
        System.out.println("Downloading JDK 17...");

        if (JDK_URL.endsWith(".zip")) {
            try (ProgressWrapper wrapper = ProgressUtil.progress(1, "Downloaded JDK 17");
                 ZipInputStream zis = new ZipInputStream(new URL(JDK_URL).openStream())) {
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
                wrapper.tick();
                wrapper.succeed();
            } catch (IOException e) {
                Files.deleteIfExists(jdkPath);
                throw e;
            }
        } else {
            try (ProgressWrapper wrapper = ProgressUtil.progress(1, "Downloaded JDK 17");
                 TarArchiveInputStream tis = new TarArchiveInputStream(
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
                wrapper.tick();
                wrapper.succeed();
            } catch (IOException e) {
                Files.deleteIfExists(getCachedJdk());
                throw e;
            }
        }
        jdkPath = getCachedJdk();
        return jdkPath;
    }
    
    public static String getJmodPath() throws IOException {
        return getJdkHome().resolve("jmods").toString();
    }

    public static String getCachedJmodPath() throws IOException {
        Path jdkPath = getCachedJdk();

        if (jdkPath == null) {
            return null;
        }
        return jdkPath.resolve("jmods").toString();
    }

    public static boolean isJdkDownloaded() {
        return jdkDownloaded;
    }
}