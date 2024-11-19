import dev.skidfuscator.pureanalysis.condition.CompositeCondition;
import dev.skidfuscator.pureanalysis.PurityAnalyzer;
import dev.skidfuscator.pureanalysis.condition.impl.MethodCallPurityCondition;
import dev.skidfuscator.pureanalysis.condition.impl.PrimitiveParametersCondition;
import dev.skidfuscator.pureanalysis.condition.impl.StaticMethodCondition;
import dev.skidfuscator.pureanalysis.condition.impl.nested.NoSideEffectsCondition;
import org.junit.jupiter.api.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

public class PurityAnalyzerTest {
    private PurityAnalyzer analyzer;
    private TestClassLoader classLoader;

    @BeforeEach
    void setUp() {
        classLoader = new TestClassLoader();
        analyzer = new PurityAnalyzer(classLoader);
        setupBasicConditions();
    }

    private void setupBasicConditions() {
        CompositeCondition rootCondition = new CompositeCondition(CompositeCondition.Operation.AND);
        
        // Add basic conditions
        rootCondition.addCondition(new StaticMethodCondition());
        rootCondition.addCondition(new PrimitiveParametersCondition());
        rootCondition.addCondition(new NoSideEffectsCondition());
        
        // Add method call condition with some pure Java methods whitelisted
        MethodCallPurityCondition callCondition = new MethodCallPurityCondition(true);
        callCondition.addWhitelistedMethod("java/lang/Math.abs(I)I");
        callCondition.addWhitelistedMethod("java/lang/Math.max(II)I");
        rootCondition.addCondition(callCondition);
        
        analyzer.addCondition(rootCondition);
    }

    // Custom ClassLoader for testing
    private static class TestClassLoader extends ClassLoader {
        private final Map<String, byte[]> customClasses = new HashMap<>();

        public void defineCustomClass(String name, byte[] bytecode) {
            customClasses.put(name, bytecode);
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            if (customClasses.containsKey(name.substring(0, name.length() - 6))) {
                return new ByteArrayInputStream(
                    customClasses.get(name.substring(0, name.length() - 6))
                );
            }
            return super.getResourceAsStream(name);
        }
    }

    private ClassNode createTestClass(String className, Consumer<ClassVisitor> classBuilder) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classBuilder.accept(cw);
        byte[] bytecode = cw.toByteArray();
        
        // Store in our test class loader
        classLoader.defineCustomClass(className, bytecode);
        
        // Create ClassNode for analysis
        ClassReader cr = new ClassReader(bytecode);
        ClassNode classNode = new ClassNode();
        cr.accept(classNode, ClassReader.EXPAND_FRAMES);
        return classNode;
    }

    @Test
    void testPurePrimitiveMethod() {
        // Create a class with a pure static method that only uses primitives
        ClassNode classNode = createTestClass("TestPure", cv -> {
            cv.visit(V1_8, ACC_PUBLIC, "TestPure", null, "java/lang/Object", null);
            
            MethodVisitor mv = cv.visitMethod(
                ACC_PUBLIC | ACC_STATIC,
                "add",
                "(II)I",
                null,
                null
            );
            
            mv.visitCode();
            mv.visitVarInsn(ILOAD, 0);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitInsn(IADD);
            mv.visitInsn(IRETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
            
            cv.visitEnd();
        });

        MethodNode methodNode = classNode.methods.stream()
            .filter(m -> m.name.equals("add"))
            .findFirst()
            .orElseThrow(IllegalStateException::new);

        assertTrue(analyzer.analyzeMethod(methodNode, classNode));
    }

    @Test
    void testImpureMethodWithStaticFieldAccess() {
        ClassNode classNode = createTestClass("TestImpure", cv -> {
            cv.visit(V1_8, ACC_PUBLIC, "TestImpure", null, "java/lang/Object", null);
            
            // Create static field
            cv.visitField(
                ACC_PRIVATE | ACC_STATIC,
                "counter",
                "I",
                null,
                null
            );
            
            // Create method that modifies static field
            MethodVisitor mv = cv.visitMethod(
                ACC_PUBLIC | ACC_STATIC,
                "increment",
                "()I",
                null,
                null
            );
            
            mv.visitCode();
            mv.visitFieldInsn(GETSTATIC, "TestImpure", "counter", "I");
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IADD);
            mv.visitFieldInsn(PUTSTATIC, "TestImpure", "counter", "I");
            mv.visitFieldInsn(GETSTATIC, "TestImpure", "counter", "I");
            mv.visitInsn(IRETURN);
            mv.visitMaxs(2, 0);
            mv.visitEnd();
            
            cv.visitEnd();
        });

        MethodNode methodNode = classNode.methods.stream()
            .filter(m -> m.name.equals("increment"))
            .findFirst()
            .orElseThrow(IllegalStateException::new);

        assertFalse(analyzer.analyzeMethod(methodNode, classNode));
    }

    @Test
    void testMethodWithWhitelistedCalls() {
        ClassNode classNode = createTestClass("TestWhitelisted", cv -> {
            cv.visit(V1_8, ACC_PUBLIC, "TestWhitelisted", null, "java/lang/Object", null);
            
            MethodVisitor mv = cv.visitMethod(
                ACC_PUBLIC | ACC_STATIC,
                "absMax",
                "(II)I",
                null,
                null
            );
            
            mv.visitCode();
            // Call Math.abs on first parameter
            mv.visitVarInsn(ILOAD, 0);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "abs", "(I)I", false);
            
            // Call Math.abs on second parameter
            mv.visitVarInsn(ILOAD, 1);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "abs", "(I)I", false);
            
            // Call Math.max on results
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "max", "(II)I", false);
            
            mv.visitInsn(IRETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
            
            cv.visitEnd();
        });

        MethodNode methodNode = classNode.methods.stream()
            .filter(m -> m.name.equals("absMax"))
            .findFirst()
            .orElseThrow(IllegalStateException::new);

        assertTrue(analyzer.analyzeMethod(methodNode, classNode));
    }

    @Test
    void testCompositeConditions() {
        // Create a composite condition that requires either static OR all parameters to be primitive
        CompositeCondition compositeCondition = new CompositeCondition(CompositeCondition.Operation.OR);
        compositeCondition.addCondition(new StaticMethodCondition());
        compositeCondition.addCondition(new PrimitiveParametersCondition());
        
        PurityAnalyzer customAnalyzer = new PurityAnalyzer(classLoader);
        customAnalyzer.addCondition(compositeCondition);

        // Test with non-static method with primitive parameters
        ClassNode classNode = createTestClass("TestComposite", cv -> {
            cv.visit(V1_8, ACC_PUBLIC, "TestComposite", null, "java/lang/Object", null);
            
            MethodVisitor mv = cv.visitMethod(
                ACC_PUBLIC, // Not static
                "add",
                "(II)I",
                null,
                null
            );
            
            mv.visitCode();
            mv.visitVarInsn(ILOAD, 1);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitInsn(IADD);
            mv.visitInsn(IRETURN);
            mv.visitMaxs(2, 3);
            mv.visitEnd();
            
            cv.visitEnd();
        });

        MethodNode methodNode = classNode.methods.stream()
            .filter(m -> m.name.equals("add"))
            .findFirst()
            .orElseThrow(IllegalStateException::new);

        assertTrue(customAnalyzer.analyzeMethod(methodNode, classNode));
    }

    @Test
    void testMethodWithRecursion() {
        ClassNode classNode = createTestClass("TestRecursion", cv -> {
            cv.visit(V1_8, ACC_PUBLIC, "TestRecursion", null, "java/lang/Object", null);
            
            // Create factorial method
            MethodVisitor mv = cv.visitMethod(
                ACC_PUBLIC | ACC_STATIC,
                "factorial",
                "(I)I",
                null,
                null
            );
            
            mv.visitCode();
            mv.visitVarInsn(ILOAD, 0);
            mv.visitInsn(ICONST_1);
            Label l1 = new Label();
            mv.visitJumpInsn(IF_ICMPGT, l1);
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IRETURN);
            mv.visitLabel(l1);
            mv.visitVarInsn(ILOAD, 0);
            mv.visitVarInsn(ILOAD, 0);
            mv.visitInsn(ICONST_1);
            mv.visitInsn(ISUB);
            mv.visitMethodInsn(INVOKESTATIC, "TestRecursion", "factorial", "(I)I", false);
            mv.visitInsn(IMUL);
            mv.visitInsn(IRETURN);
            mv.visitMaxs(3, 1);
            mv.visitEnd();
            
            cv.visitEnd();
        });

        MethodNode methodNode = classNode.methods.stream()
            .filter(m -> m.name.equals("factorial"))
            .findFirst()
            .orElseThrow(IllegalStateException::new);

        assertTrue(analyzer.analyzeMethod(methodNode, classNode));
    }
}