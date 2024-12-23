package dev.skidfuscator.obfuscator.io.apk;

import dev.skidfuscator.obfuscator.Skidfuscator;
import dev.skidfuscator.obfuscator.io.dex.DexInputSource;
import org.topdank.byteengineer.commons.data.JarContents;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ApkInputSource extends DexInputSource {

    public ApkInputSource(Skidfuscator skidfuscator) {
        super(skidfuscator);
    }

    @Override
    public JarContents download(final File input) {
        final JarContents contents = new JarContents();

        try {
            Path tempDir = Paths.get("tempDex");
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
            }


            List<String> dexFiles = extractDexFiles(input, tempDir.toString());
            for (String dexFile : dexFiles) {
                JarContents localContents = processDexFile(dexFile);
                contents.add(localContents);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contents;
    }

    private List<String> extractDexFiles(File apkFile, String outputDir) throws IOException {
        if (!apkFile.exists()) {
            throw new IOException("APK file not found: " + apkFile.getAbsolutePath());
        }

        File outputDirectory = new File(outputDir);
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new IOException("Failed to create output directory: " + outputDir);
        }

        List<String> extractedFiles = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(apkFile)) {
            zipFile.stream()
                    .filter(entry -> entry.getName().endsWith(".dex"))
                    .forEach(entry -> {
                        try {
                            String extractedPath = extractDexFile(zipFile, entry, outputDirectory);
                            extractedFiles.add(extractedPath);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to extract " + entry.getName(), e);
                        }
                    });
        }
        return extractedFiles;
    }

    private String extractDexFile(ZipFile zipFile, ZipEntry dexEntry, File outputDir) throws IOException {
        File outputFile = new File(outputDir, dexEntry.getName());
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create directory: " + parent.getPath());
        }

        try (InputStream input = zipFile.getInputStream(dexEntry);
             FileOutputStream output = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        }
        return outputFile.getAbsolutePath();
    }

    private JarContents processDexFile(String dexFile) {
        DexInputSource dexInputSource = new DexInputSource(skidfuscator);
        return dexInputSource.download(new File(dexFile));
    }
}