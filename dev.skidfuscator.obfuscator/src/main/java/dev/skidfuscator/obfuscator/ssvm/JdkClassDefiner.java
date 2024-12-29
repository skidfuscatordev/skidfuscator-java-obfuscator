package dev.skidfuscator.obfuscator.ssvm;

import dev.skidfuscator.obfuscator.util.JdkDownloader;
import dev.xdark.ssvm.classloading.ClassDefiner;
import dev.xdark.ssvm.classloading.ParsedClassData;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JdkClassDefiner extends DelegatingClassDefiner {
    private final Path jmodDir;
    private final Map<String, ZipFile> moduleCache = new HashMap<>();
    
    public JdkClassDefiner(ClassDefiner delegate) throws IOException {
        super(delegate);
        this.jmodDir = JdkDownloader.getJdkHome().resolve("jmods");
        if (!Files.exists(jmodDir)) {
            throw new IOException("JDK jmods directory not found: " + jmodDir);
        }
    }
    
    @Override
    public ParsedClassData parseClass(String name, byte[] bytes, int off, int len, String source) {
        // For bootstrap classes, try to load from our JDK first
        //System.out.println("Loading " + name + " from " + source);
        if (name != null && (name.startsWith("java/") || name.startsWith("jdk/"))) {
            System.out.println("Loading " + name + " from jmod");
            try {
                byte[] jdkBytes = loadFromJmod(name);
                if (jdkBytes != null) {
                    return super.parseClass(name, jdkBytes, 0, jdkBytes.length, "jmod:" + source);
                }
            } catch (IOException e) {
                // Fall through to original bytes
                System.err.println("Failed to load " + name + " from jmod: " + e.getMessage());
            }
        }
        return super.parseClass(name, bytes, off, len, source);
    }
    
    private byte[] loadFromJmod(String name) throws IOException {
        // Common modules to check first
        //System.out.println("Loading " + name + " from jmod");
        String[] modulesToCheck = {
            "java.base.jmod",
            "java.desktop.jmod",
            "jdk.internal.jvmci.jmod",
            "java.management.jmod",

        };
        
        String classPath = "classes/" + name + ".class";
        
        // Check common modules first
        for (String module : modulesToCheck) {
            byte[] result = loadFromModule(module, classPath);
            if (result != null) {
                return result;
            }
        }
        
        // If not found in common modules, check all jmod files
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(jmodDir, "*.jmod")) {
            for (Path jmodPath : stream) {
                String moduleName = jmodPath.getFileName().toString();
                if (!moduleCache.containsKey(moduleName)) {
                    byte[] result = loadFromModule(moduleName, classPath);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        
        return null;
    }
    
    private byte[] loadFromModule(String moduleName, String classPath) throws IOException {
        ZipFile zip = moduleCache.computeIfAbsent(moduleName, name -> {
            try {
                return new ZipFile(jmodDir.resolve(name).toFile());
            } catch (IOException e) {
                return null;
            }
        });
        
        if (zip != null) {
            ZipEntry entry = zip.getEntry(classPath);
            if (entry != null) {
                try (InputStream in = zip.getInputStream(entry)) {
                    return in.readAllBytes();
                }
            }
        }
        
        return null;
    }
    
    @Override
    protected void finalize() throws Throwable {
        // Close all cached ZipFiles
        for (ZipFile zip : moduleCache.values()) {
            if (zip != null) {
                zip.close();
            }
        }
        super.finalize();
    }
}