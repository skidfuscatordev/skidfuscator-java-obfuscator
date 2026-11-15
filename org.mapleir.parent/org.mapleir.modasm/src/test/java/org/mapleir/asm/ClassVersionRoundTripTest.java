package org.mapleir.asm;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

/**
 * Guards the obfuscator's ability to read and write modern class-file
 * versions. The underlying ASM {@link ClassReader} refuses any major
 * version newer than the one it was built for, so this is the single
 * point that historically capped Skidfuscator at a given Java release.
 *
 * <p>Java 17 is class-file major version 61 and Java 25 is major version
 * 69. These tests make sure the {@link ClassHelper} read/write round-trip
 * accepts a Java 25 class and preserves its version, catching any
 * accidental ASM downgrade below Java 25 support.
 */
public class ClassVersionRoundTripTest {

    /** Class-file major version for Java 17. */
    private static final int V_JAVA_17 = 61;
    /** Class-file major version for Java 25. */
    private static final int V_JAVA_25 = 69;

    /**
     * Builds a trivial {@code public static int meaning()} class at the
     * requested class-file major version.
     */
    private static byte[] buildClass(final String internalName, final int major) {
        final ClassWriter cw = new ClassWriter(0);
        // minor version 0, requested major version
        cw.visit(major, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                internalName, null, "java/lang/Object", null);

        final org.objectweb.asm.MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "meaning", "()I", null, null);
        mv.visitCode();
        mv.visitIntInsn(Opcodes.BIPUSH, 42);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(1, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    private static int majorVersion(final byte[] clazz) {
        return ((clazz[6] & 0xFF) << 8) | (clazz[7] & 0xFF);
    }

    @Test
    public void readsJava25ClassFiles() {
        final byte[] java25 = buildClass("probe/Java25Probe", V_JAVA_25);
        assertEquals(V_JAVA_25, majorVersion(java25), "test fixture should be Java 25");

        // The historical Java-version cap lived here: ClassReader throws
        // IllegalArgumentException for a major version it does not support.
        assertDoesNotThrow(() -> ClassHelper.create(java25),
                "ASM must support reading Java 25 (major 69) class files");
    }

    @Test
    public void roundTripPreservesJava25Version() {
        final byte[] java25 = buildClass("probe/Java25Probe", V_JAVA_25);

        final ClassNode node = ClassHelper.create(java25);
        assertEquals(V_JAVA_25, node.node.version & 0xFFFF,
                "parsed node should keep the Java 25 version");

        final byte[] rewritten = ClassHelper.toByteArray(node);
        assertTrue(rewritten.length > 0, "rewrite should produce bytes");
        assertEquals(V_JAVA_25, majorVersion(rewritten),
                "write-back must preserve the Java 25 class-file version");

        // The rewritten bytes must still be readable by the pipeline.
        assertDoesNotThrow(() -> new ClassReader(rewritten).getClassName(),
                "rewritten Java 25 class must remain parseable");
    }

    @Test
    public void stillHandlesJava17ClassFiles() {
        final byte[] java17 = buildClass("probe/Java17Probe", V_JAVA_17);

        final ClassNode node = ClassHelper.create(java17);
        final byte[] rewritten = ClassHelper.toByteArray(node);
        assertEquals(V_JAVA_17, majorVersion(rewritten),
                "Java 17 support must be retained alongside Java 25");
    }
}
