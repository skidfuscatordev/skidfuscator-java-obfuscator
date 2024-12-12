package com.example.dependencyanalyzer;

import dev.skidfuscator.dependanalysis.DependencyClassHierarchy;
import dev.skidfuscator.dependanalysis.DependencyResult;
import dev.skidfuscator.dependanalysis.visitor.HierarchyVisitor;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * The DependencyAnalyzer uses OW2 ASM to determine the minimal set of dependency jars
 * required for a given main jar's class hierarchy. It returns a structured 
 * DependencyResult object that encapsulates the necessary jars, classes, and reasons.
 */
public class DependencyAnalyzer {
    private final Path mainJar;
    private final Path librariesDir;

    // Maps className to the jar that hosts it (for library jars)
    private final Map<String, Path> classToLibraryMap = new HashMap<>();
    // Cache of previously computed hierarchies
    private final Map<String, DependencyClassHierarchy> cache = new HashMap<>();

    // For reporting: map from jar -> map of className -> list of reasons
    private final Map<Path, Map<String, List<String>>> dependencyReport = new LinkedHashMap<>();

    public DependencyAnalyzer(Path mainJar, Path librariesDir) {
        this.mainJar = mainJar;
        this.librariesDir = librariesDir;
    }

    /**
     * Analyze the main jar’s classes, build hierarchies, and determine required library jars.
     * Returns a structured DependencyResult object.
     */
    public DependencyResult analyze() throws IOException {
        // Step 1: Index library jars
        indexLibraries();

        // Step 2: Load all classes from main jar
        Set<String> mainClasses = loadClassesFromJar(mainJar);

        // Step 3: Resolve hierarchical dependencies
        Set<Path> requiredJars = new LinkedHashSet<>();
        for (String cls : mainClasses) {
            // top-level classes from main jar have a general reason
            resolveHierarchy(cls, requiredJars, mainJar, new HashSet<>(), "top-level class from main jar");
        }

        return buildResult(requiredJars);
    }

    /**
     * Recursively resolves the class hierarchy for a given class, updating requiredJars
     * and the dependency report as external classes are found.
     *
     * @param className The class to resolve
     * @param requiredJars Set of jars already identified as required
     * @param sourceJar The jar in which we expect to find this class
     * @param visited Set of visited classes to avoid cycles
     * @param reason A textual reason describing why we are resolving this class
     */
    private void resolveHierarchy(String className,
                                  Set<Path> requiredJars,
                                  Path sourceJar,
                                  Set<String> visited,
                                  String reason) throws IOException {
        if (visited.contains(className)) return;
        visited.add(className);

        DependencyClassHierarchy hierarchy = loadDependencyClassHierarchy(className, sourceJar);

        // If class is from a library jar, record the reason
        if (!hierarchy.isMainJarClass && hierarchy.sourceJar != null) {
            requiredJars.add(hierarchy.sourceJar);
            addToReport(hierarchy.sourceJar, hierarchy.className, reason);
        }

        // Resolve superclass
        if (hierarchy.superName != null && !hierarchy.superName.isEmpty()) {
            Path jarForSuper = hierarchy.isMainJarClass ? mainJar : classToLibraryMap.get(hierarchy.superName);
            if (jarForSuper == null && hierarchy.superName != null) {
                jarForSuper = classToLibraryMap.get(hierarchy.superName);
            }
            if (jarForSuper != null) {
                String superReason = "needed as superclass of " + className;
                resolveHierarchy(hierarchy.superName, requiredJars, jarForSuper, visited, superReason);
            }
        }

        // Resolve interfaces
        for (String iface : hierarchy.interfaces) {
            Path jarForIface = hierarchy.isMainJarClass ? mainJar : classToLibraryMap.get(iface);
            if (jarForIface == null && iface != null) {
                jarForIface = classToLibraryMap.get(iface);
            }
            if (jarForIface != null) {
                String ifaceReason = "needed as an interface of " + className;
                resolveHierarchy(iface, requiredJars, jarForIface, visited, ifaceReason);
            }
        }
    }

    /**
     * Construct the final DependencyResult object from the gathered report data.
     */
    private DependencyResult buildResult(Set<Path> requiredJars) {
        List<DependencyResult.JarDependency> jarDependencies = new ArrayList<>();
        for (Path jarPath : requiredJars) {
            Map<String, List<String>> classesMap = dependencyReport.getOrDefault(jarPath, Collections.emptyMap());
            List<DependencyResult.ClassDependency> classDependencies = new ArrayList<>();

            for (Map.Entry<String, List<String>> entry : classesMap.entrySet()) {
                classDependencies.add(new DependencyResult.ClassDependency(entry.getKey(), entry.getValue()));
            }

            jarDependencies.add(new DependencyResult.JarDependency(jarPath, classDependencies));
        }

        return new DependencyResult(jarDependencies);
    }

    /**
     * Load the class hierarchy of a given class. If cached, use the cache.
     */
    private DependencyClassHierarchy loadDependencyClassHierarchy(String className, Path presumedJar) throws IOException {
        if (cache.containsKey(className)) {
            return cache.get(className);
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
                // Not found anywhere
                DependencyClassHierarchy notFound = new DependencyClassHierarchy(className, null, new String[0], true, null);
                cache.put(className, notFound);
                return notFound;
            }
            classStream = getClassStream(libJar, className);
            jarSource = libJar;
        }

        if (classStream == null) {
            // Not found anywhere
            DependencyClassHierarchy notFound = new DependencyClassHierarchy(className, null, new String[0], true, null);
            cache.put(className, notFound);
            return notFound;
        }

        ClassReader cr = new ClassReader(classStream);
        HierarchyVisitor visitor = new HierarchyVisitor();
        cr.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        DependencyClassHierarchy hierarchy = new DependencyClassHierarchy(
                className,
                visitor.superName,
                visitor.interfaces,
                fromMainJar,
                fromMainJar ? null : jarSource
        );
        cache.put(className, hierarchy);
        return hierarchy;
    }

    /**
     * Index all library jars by their classes.
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
     * Get the InputStream of the specified class from the given jar.
     */
    private InputStream getClassStream(Path jar, String className) throws IOException {
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
     * Add an entry to the dependency report for the given jar and class.
     */
    private void addToReport(Path jar, String className, String reason) {
        dependencyReport.computeIfAbsent(jar, k -> new LinkedHashMap<>());
        Map<String, List<String>> classes = dependencyReport.get(jar);
        classes.computeIfAbsent(className, k -> new ArrayList<>());
        classes.get(className).add(reason);
    }

    /**
     * A wrapper that ensures the JarFile is closed when the InputStream is closed.
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
