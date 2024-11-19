package framework;

import dev.skidfuscator.pureanalysis.PurityAnalyzer;
import dev.skidfuscator.pureanalysis.condition.CompositeCondition;
import dev.skidfuscator.pureanalysis.condition.impl.PrimitiveParametersCondition;
import dev.skidfuscator.pureanalysis.condition.impl.StaticMethodCondition;
import dev.skidfuscator.pureanalysis.condition.impl.ArrayStateCondition;
import dev.skidfuscator.pureanalysis.condition.impl.nested.NoSideEffectsCondition;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class PurityAnalysisTest {
    private static final Path TEST_CLASSES_PATH = Paths.get("src/test/java/framework/testclasses");
    private final PurityAnalyzer analyzer;
    private final List<MethodTestInfo> testMethods = new ArrayList<>();
    private final MemoryClassLoader classLoader = new MemoryClassLoader();

    public PurityAnalysisTest() {
        this.analyzer = new PurityAnalyzer(classLoader);
        setupAnalyzer();
    }

    private void setupAnalyzer() {
        CompositeCondition rootCondition = new CompositeCondition(CompositeCondition.Operation.AND);
        rootCondition.addCondition(new StaticMethodCondition());
        rootCondition.addCondition(new PrimitiveParametersCondition());
        rootCondition.addCondition(new NoSideEffectsCondition());
        rootCondition.addCondition(new ArrayStateCondition());
        analyzer.addCondition(rootCondition);
    }

    @TestFactory
    Stream<DynamicTest> purityTests() throws IOException {
        compileAndScanTestClasses();
        return testMethods.stream()
                .map(this::createTestForMethod);
    }

    private void compileAndScanTestClasses() throws IOException {
        // Find all Java source files
        List<JavaSourceFromString> sourceFiles = new ArrayList<>();
        Files.walk(TEST_CLASSES_PATH)
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(path -> {
                    try {
                        String className = getClassNameFromPath(path);
                        String sourceCode = Files.readString(path);
                        sourceFiles.add(new JavaSourceFromString(className, sourceCode));
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read source file: " + path, e);
                    }
                });

        // Compile the source files
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        MemoryJavaFileManager fileManager = new MemoryJavaFileManager(
                compiler.getStandardFileManager(diagnostics, null, null));

        JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, diagnostics, null, null, sourceFiles);

        if (!task.call()) {
            throw new RuntimeException("Compilation failed: " +
                    diagnostics.getDiagnostics().stream()
                            .map(Object::toString)
                            .reduce("", (a, b) -> a + "\n" + b));
        }

        // Process compiled classes
        Map<String, byte[]> compiledClasses = fileManager.getClassBytes();
        compiledClasses.forEach((name, bytes) -> {
            classLoader.addClass(name, bytes);
            processCompiledClass(name, bytes);
        });
    }

    private String getClassNameFromPath(Path path) {
        Path relativePath = TEST_CLASSES_PATH.relativize(path);
        String pathStr = relativePath.toString();
        return pathStr.substring(0, pathStr.length() - 5) // remove .java
                .replace(File.separatorChar, '.');
    }

    private void processCompiledClass(String className, byte[] classBytes) {
        try {
            ClassReader reader = new ClassReader(classBytes);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, ClassReader.EXPAND_FRAMES);

            Class<?> originalClass = classLoader.loadClass(className);

            for (MethodNode methodNode : classNode.methods) {
                if (!methodNode.name.equals("<init>") && !methodNode.name.equals("<clinit>")) {
                    processMethod(originalClass, classNode, methodNode);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to process class: " + className, e);
        }
    }

    private void processMethod(Class<?> originalClass, ClassNode classNode, MethodNode methodNode) {
        // Skip constructors and synthetic methods
        if (methodNode.name.equals("<init>") || methodNode.name.equals("<clinit>")) {
            return;
        }

        try {
            java.lang.reflect.Method originalMethod = findOriginalMethod(originalClass, methodNode);
            if (originalMethod == null) return;

            Pure pureAnnotation = originalMethod.getAnnotation(Pure.class);
            Impure impureAnnotation = originalMethod.getAnnotation(Impure.class);

            if (pureAnnotation != null || impureAnnotation != null) {
                testMethods.add(new MethodTestInfo(
                        originalClass.getName(),
                        methodNode.name,
                        methodNode.desc,
                        pureAnnotation != null,
                        pureAnnotation != null ? pureAnnotation.description() : impureAnnotation.description(),
                        pureAnnotation != null ? pureAnnotation.because() : impureAnnotation.because(),
                        classNode,
                        methodNode
                ));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to process method: " + methodNode.name, e);
        }
    }

    private DynamicTest createTestForMethod(MethodTestInfo testInfo) {
        return dynamicTest(
                testInfo.getDisplayName(),
                () -> verifyMethodPurity(testInfo)
        );
    }

    private void verifyMethodPurity(MethodTestInfo testInfo) {
        boolean actualPure = analyzer.analyzeMethod(testInfo.methodNode, testInfo.classNode);

        String message = buildFailureMessage(testInfo, actualPure);

        if (testInfo.expectedPure) {
            assertTrue(actualPure, message);
        } else {
            assertFalse(actualPure, message);
        }
    }

    private String buildFailureMessage(MethodTestInfo testInfo, boolean actualPure) {
        StringBuilder sb = new StringBuilder();
        sb.append("\nMethod purity analysis failed for: ")
                .append(testInfo.className)
                .append("#")
                .append(testInfo.methodName)
                .append(testInfo.descriptor);

        sb.append("\nExpected: ").append(testInfo.expectedPure ? "pure" : "impure");
        sb.append("\nActual: ").append(actualPure ? "pure" : "impure");

        if (!testInfo.description.isEmpty()) {
            sb.append("\nDescription: ").append(testInfo.description);
        }

        if (testInfo.reasons.length > 0) {
            sb.append("\nReasons:");
            for (String reason : testInfo.reasons) {
                sb.append("\n  - ").append(reason);
            }
        }

        return sb.toString();
    }

    private java.lang.reflect.Method findOriginalMethod(Class<?> clazz, MethodNode methodNode) {
        try {
            Type[] argumentTypes = Type.getArgumentTypes(methodNode.desc);
            Class<?>[] parameterTypes = new Class<?>[argumentTypes.length];
            for (int i = 0; i < argumentTypes.length; i++) {
                parameterTypes[i] = getClassFromType(argumentTypes[i]);
            }
            return clazz.getDeclaredMethod(methodNode.name, parameterTypes);
        } catch (Exception e) {
            return null;
        }
    }

    private Class<?> getClassFromType(Type type) throws ClassNotFoundException {
        switch (type.getSort()) {
            case Type.BOOLEAN: return boolean.class;
            case Type.CHAR: return char.class;
            case Type.BYTE: return byte.class;
            case Type.SHORT: return short.class;
            case Type.INT: return int.class;
            case Type.FLOAT: return float.class;
            case Type.LONG: return long.class;
            case Type.DOUBLE: return double.class;
            case Type.ARRAY: return Class.forName(type.getDescriptor().replace('/', '.'));
            case Type.OBJECT: return Class.forName(type.getClassName());
            default: throw new ClassNotFoundException("Unknown type: " + type);
        }
    }
}