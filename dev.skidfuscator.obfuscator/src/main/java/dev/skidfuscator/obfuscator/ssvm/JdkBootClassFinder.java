package dev.skidfuscator.obfuscator.ssvm;

import dev.skidfuscator.obfuscator.util.JdkDownloader;
import dev.xdark.ssvm.classloading.BootClassFinder;
import dev.xdark.ssvm.classloading.ParsedClassData;
import dev.xdark.ssvm.util.ClassUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JdkBootClassFinder implements BootClassFinder {
    private final Path jmodDir;
    private final Map<String, ZipFile> moduleCache = new HashMap<>();
    private final BootClassFinder delegate;

    public JdkBootClassFinder(BootClassFinder delegate) throws IOException {
        this.delegate = delegate;
        this.jmodDir = JdkDownloader.getJdkHome().resolve("jmods");
        if (!Files.exists(jmodDir)) {
            throw new IOException("JDK jmods directory not found: " + jmodDir);
        }
    }

    @Override
    public ParsedClassData findBootClass(String name) {
        // For bootstrap classes, try to load from our JDK first
        if (name != null && (name.startsWith("java/") || name.startsWith("jdk/") || name.startsWith("sun/"))) {
            try {
                byte[] jdkBytes = loadFromJmod(name);
                if (jdkBytes != null) {
                    ClassReader cr = new ClassReader(jdkBytes);
                    ClassNode node = ClassUtil.readNode(cr);
                    //System.out.println("[!!] Loading " + name + " from jmod");
                    return new ParsedClassData(cr, node);
                }
            } catch (IOException e) {
                // Fall through to delegate
                System.err.println("Failed to load " + name + " from jmod: " + e.getMessage());
            }
        }

        //System.out.println("[??] Loading " + name + " from " + delegate.getClass().getName());
        
        // If not found in JDK, delegate to original finder
        return null;
    }

    private byte[] loadFromJmod(String name) throws IOException {
        // Common modules to check first
        //System.out.println("Loading " + name + " from jmod");
        String[] modulesToCheck = {
            "java.base.jmod",
            "java.desktop.jmod",
            "java.management.jmod"
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
                //System.out.println("Checking " + moduleName);
                byte[] result = loadFromModule(moduleName, classPath);
                if (result != null) {
                    return result;
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
            //System.out.println("Loading entry " + classPath + " from jmod " + moduleName);
            ZipEntry entry = zip.getEntry(classPath);
            if (entry != null) {
                try (InputStream in = zip.getInputStream(entry)) {
                    return in.readAllBytes();
                }
            }
        } else {
            System.out.println("Failed to load " + moduleName);
            throw new IllegalStateException("Failed to load " + moduleName);
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