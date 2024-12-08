import dev.skidfuscator.pureanalysis.PurityAnalyzer;
import dev.skidfuscator.pureanalysis.PurityReport;
import dev.skidfuscator.pureanalysis.SimpleClassHierarchyAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

public class JVMPurityAnalyzerTest {
    private PurityAnalyzer analyzer;
    private final Set<String> analyzedJars = new HashSet<>();
    private final Map<String, Boolean> methodResults = new HashMap<>();

    @BeforeEach
    void setUp() {
        analyzer = new PurityAnalyzer(new SimpleClassHierarchyAnalyzer(
                getClass().getClassLoader())
        );
    }

    @Test
    void analyzeJVMPureFunctions() {
        // Get Java home directory
        String javaHome = System.getProperty("java.home");
        File javaHomeDir = new File(javaHome);
        
        System.out.println("Analyzing JVM classes in: " + javaHome);

        // Process rt.jar or modular JDK jars
        try {
            if (isModularJDK(javaHomeDir)) {
                analyzeModularJDK(javaHomeDir);
            } else {
                File rtJar = new File(javaHomeDir, "lib/rt.jar");
                if (rtJar.exists()) {
                    analyzeJarFile(rtJar);
                }
            }

            // Print results
            printAnalysisResults();

        } catch (IOException e) {
            fail("Failed to analyze JVM classes: " + e.getMessage());
        }
    }

    private boolean isModularJDK(File javaHome) {
        return new File(javaHome, "jmods").exists();
    }

    private void analyzeModularJDK(File javaHome) throws IOException {
        File jmodsDir = new File(javaHome, "jmods");
        File libDir = new File(javaHome, "lib");

        // Analyze jmod files if they exist
        if (jmodsDir.exists()) {
            File[] jmodFiles = jmodsDir.listFiles((dir, name) -> name.endsWith(".jmod"));
            if (jmodFiles != null) {
                for (File jmodFile : jmodFiles) {
                    analyzeJarFile(jmodFile);
                }
            }
        }

        // Analyze jar files in lib directory
        if (libDir.exists()) {
            File[] jarFiles = libDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jarFiles != null) {
                for (File jarFile : jarFiles) {
                    analyzeJarFile(jarFile);
                }
            }
        }
    }

    private void analyzeJarFile(File jarFile) throws IOException {
        if (!analyzedJars.add(jarFile.getAbsolutePath())) {
            return; // Skip if already analyzed
        }

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) {
                    continue;
                }

                // Skip module-info, package-info, and similar files
                if (entry.getName().contains("module-info") || 
                    entry.getName().contains("package-info")) {
                    continue;
                }

                try {
                    analyzeClassEntry(jar, entry);
                } catch (Exception e) {
                    System.err.println("Failed to analyze: " + entry.getName() + " - " + e.getMessage());
                }
            }
        }
    }

    private void analyzeClassEntry(JarFile jar, JarEntry entry) throws IOException {
        ClassReader reader = new ClassReader(jar.getInputStream(entry));
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        // Skip synthetic and bridge methods
        for (MethodNode method : classNode.methods) {
            if ((method.access & (ACC_SYNTHETIC | ACC_BRIDGE)) != 0) {
                continue;
            }

            String methodKey = classNode.name + "." + method.name + method.desc;
            try {
                PurityReport purity = analyzer.analyzeMethodPurity(method, classNode);
                methodResults.put(methodKey, purity.isPure());
            } catch (Exception e) {
                System.err.println("Failed to analyze method: " + methodKey + " - " + e.getMessage());
            }
        }
    }

    private void printAnalysisResults() {
        System.out.println("\nAnalysis Results:");
        System.out.println("Total methods analyzed: " + methodResults.size());

        // Count pure methods
        long pureCount = methodResults.values().stream().filter(Boolean::booleanValue).count();
        System.out.println("Pure methods found: " + pureCount);

        // Print pure methods by package
        Map<String, List<String>> pureMethodsByPackage = new TreeMap<>();

        methodResults.forEach((method, isPure) -> {
            if (isPure) {
                String packageName = method.substring(0, method.lastIndexOf('.'));
                pureMethodsByPackage
                    .computeIfAbsent(packageName, k -> new ArrayList<>())
                    .add(method);
            }
        });

        System.out.println("\nPure methods by package:");
        pureMethodsByPackage.forEach((packageName, methods) -> {
            System.out.println("\nPackage: " + packageName);
            methods.stream()
                .sorted()
                .forEach(method -> System.out.println("  " + formatMethodSignature(method)));
        });

        // Print some interesting statistics
        System.out.println("\nInteresting pure method statistics:");
        printMethodStatistics();
    }

    private String formatMethodSignature(String fullSignature) {
        // Extract method name and descriptor
        int lastSlash = fullSignature.lastIndexOf('/');
        int methodStart = fullSignature.indexOf('.', lastSlash);
        String methodName = fullSignature.substring(methodStart + 1);
        
        // Format parameters and return type
        String desc = methodName.substring(methodName.indexOf('('));
        methodName = methodName.substring(0, methodName.indexOf('('));
        
        return methodName + " " + formatMethodDescriptor(desc);
    }

    private String formatMethodDescriptor(String desc) {
        // Simple descriptor formatter
        return desc.replace('/', '.')
                  .replace("L", "")
                  .replace(';', ' ')
                  .trim();
    }

    private void printMethodStatistics() {
        // Count methods by return type
        Map<String, Integer> returnTypeStats = new HashMap<>();
        methodResults.forEach((method, isPure) -> {
            if (isPure) {
                String desc = method.substring(method.indexOf('('));
                String returnType = desc.substring(desc.indexOf(')') + 1);
                returnTypeStats.merge(returnType, 1, Integer::sum);
            }
        });

        System.out.println("\nPure methods by return type:");
        returnTypeStats.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(entry -> System.out.println(
                formatType(entry.getKey()) + ": " + entry.getValue()
            ));
    }

    private String formatType(String typeDesc) {
        switch (typeDesc) {
            case "I": return "int";
            case "J": return "long";
            case "D": return "double";
            case "F": return "float";
            case "Z": return "boolean";
            case "B": return "byte";
            case "C": return "char";
            case "S": return "short";
            case "V": return "void";
            default: return typeDesc.replace('/', '.')
                                  .replace("L", "")
                                  .replace(';', ' ')
                                  .trim();
        }
    }
}