package dev.skidfuscator.dependanalysis;

import dev.skidfuscator.dependanalysis.visitor.HierarchyVisitor;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Due to the nature of the Java Virtual Machine and the wealth of tools offered by OW2 ASM, we can 
 * analyze the hierarchy of classes within a given JAR and selectively load only the minimal set of 
 * dependencies required. By parsing the main JAR’s class definitions and walking up its hierarchy 
 * chain, we identify which subset of external JARs is truly needed.
 *
 * This class orchestrates the resolution by:
 *  - Indexing the classes found in a directory of library JARs.
 *  - Parsing the main JAR’s classes and discovering their superclasses and implemented interfaces.
 *  - Recursively climbing the class hierarchy to find any library JAR that must be included.
 */
public class DependencyAnalyzer {
    private final Path mainJar;
    private final Path librariesDir;

    // Maps className to the jar that hosts it (for library jars)
    private final Map<String, Path> classToLibraryMap = new HashMap<>();
    // Cache of previously computed hierarchies to avoid re-analysis
    private final Map<String, DependencyClassHierarchy> classHierarchyCache = new HashMap<>();

    public DependencyAnalyzer(Path mainJar, Path librariesDir) {
        this.mainJar = mainJar;
        this.librariesDir = librariesDir;
    }

    /**
     * Analyze the main jar’s classes, build their hierarchies, and return 
     * the minimal set of library jars required to resolve the entire chain.
     */
    public Set<Path> analyze() throws IOException {
        // Step 1: Index library jars
        indexLibraries();

        // Step 2: Get all classes from main jar
        Set<String> mainClasses = loadClassesFromJar(mainJar);

        // Step 3: Resolve hierarchical dependencies
        Set<Path> requiredJars = new HashSet<>();
        for (String cls : mainClasses) {
            resolveHierarchy(cls, requiredJars, mainJar, new HashSet<>());
        }
        return requiredJars;
    }

    /**
     * Recursively resolves the hierarchy of a given class, adding necessary jars as discovered.
     */
    private void resolveHierarchy(String className, Set<Path> requiredJars, Path sourceJar, Set<String> visited) throws IOException {
        if (visited.contains(className)) return;
        visited.add(className);

        DependencyClassHierarchy hierarchy = loadClassHierarchy(className, sourceJar);

        // If we found a class from a library jar
        if (!hierarchy.isMainJarClass && hierarchy.sourceJar != null) {
            requiredJars.add(hierarchy.sourceJar);
        }

        // Resolve superclass
        if (hierarchy.superName != null && !hierarchy.superName.isEmpty()) {
            Path jarForSuper = hierarchy.isMainJarClass ? mainJar : classToLibraryMap.get(hierarchy.superName);
            if (jarForSuper == null && hierarchy.superName != null) {
                jarForSuper = classToLibraryMap.get(hierarchy.superName);
            }
            if (jarForSuper != null) {
                resolveHierarchy(hierarchy.superName, requiredJars, jarForSuper, visited);
            }
        }

        // Resolve interfaces
        for (String iface : hierarchy.interfaces) {
            Path jarForIface = hierarchy.isMainJarClass ? mainJar : classToLibraryMap.get(iface);
            if (jarForIface == null && iface != null) {
                jarForIface = classToLibraryMap.get(iface);
            }
            if (jarForIface != null) {
                resolveHierarchy(iface, requiredJars, jarForIface, visited);
            }
        }
    }

    /**
     * Load the class hierarchy for a given class. If cached, return the cache.
     * Otherwise, parse from either the main jar or a known library jar.
     */
    private DependencyClassHierarchy loadClassHierarchy(String className, Path presumedJar) throws IOException {
        if (classHierarchyCache.containsKey(className)) {
            return classHierarchyCache.get(className);
        }

        boolean fromMainJar = false;
        InputStream classStream = getClassStream(mainJar, className);
        Path jarSource = null;
        if (classStream != null) {
            fromMainJar = true;
            jarSource = mainJar;
        } else {
            Path libJar = classToLibraryMap.get(className);
            if (libJar == null) {
                // Not found in known jars
                DependencyClassHierarchy notFound = new DependencyClassHierarchy(className, null, new String[0], true, null);
                classHierarchyCache.put(className, notFound);
                return notFound;
            }
            classStream = getClassStream(libJar, className);
            jarSource = libJar;
        }

        if (classStream == null) {
            DependencyClassHierarchy notFound = new DependencyClassHierarchy(className, null, new String[0], true, null);
            classHierarchyCache.put(className, notFound);
            return notFound;
        }

        ClassReader cr = new ClassReader(classStream);
        HierarchyVisitor visitor = new HierarchyVisitor();
        cr.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE);

        DependencyClassHierarchy hierarchy = new DependencyClassHierarchy(
            className, 
            visitor.superName, 
            visitor.interfaces, 
            fromMainJar,
            fromMainJar ? null : jarSource
        );
        classHierarchyCache.put(className, hierarchy);
        return hierarchy;
    }

    /**
     * Index all library jars found in librariesDir by their contained classes.
     */
    private void indexLibraries() throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(librariesDir, "*.jar")) {
            for (Path jar : stream) {
                try (JarFile jarFile = new JarFile(jar.toFile())) {
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                            String className = entry.getName().replace('/', '.').replace(".class", "");
                            classToLibraryMap.put(className, jar);
                        }
                    }
                }
            }
        }
    }

    /**
     * Load all classes from a given jar.
     */
    private Set<String> loadClassesFromJar(Path jarPath) throws IOException {
        Set<String> classes = new HashSet<>();
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    String className = entry.getName().replace('/', '.').replace(".class", "");
                    classes.add(className);
                }
            }
        }
        return classes;
    }

    /**
     * Retrieve an InputStream for a specified class from a given jar.
     */
    private InputStream getClassStream(Path jar, String className) throws IOException {
        // Need a fresh stream for each read attempt
        JarFile jf = new JarFile(jar.toFile());
        String path = className.replace('.', '/') + ".class";
        JarEntry entry = jf.getJarEntry(path);
        if (entry == null) {
            jf.close();
            return null;
        }
        return new ClosableInputStreamWrapper(jf, jf.getInputStream(entry));
    }

    /**
     * A wrapper that closes the JarFile once the InputStream is closed.
     */
    private static class ClosableInputStreamWrapper extends InputStream {
        private final JarFile jarFile;
        private final InputStream delegate;

        public ClosableInputStreamWrapper(JarFile jarFile, InputStream delegate) {
            this.jarFile = jarFile;
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            return delegate.read();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
            jarFile.close();
        }
    }
}
