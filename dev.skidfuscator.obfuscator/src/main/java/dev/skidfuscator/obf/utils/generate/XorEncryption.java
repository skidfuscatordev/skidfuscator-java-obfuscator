package dev.skidfuscator.obf.utils.generate;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.nio.charset.StandardCharsets;

import static org.objectweb.asm.Opcodes.*;

public class XorEncryption {
    public static final String NAME = "doTheSkiddusFunnius$123";

    public static String factor(final String string, int key) {
        final byte[] encrypted = string.getBytes();
        final byte[] modulo = Integer.toString(key).getBytes();

        for (int i = 0; i < encrypted.length; i++) {
            encrypted[i] = (byte) (encrypted[i] ^ modulo[i % modulo.length]);
        }

        return new String(encrypted);
    }

    public static void visit(final ClassNode node) {
        final MethodVisitor methodVisitor = node.visitMethod(ACC_PRIVATE | ACC_STATIC, NAME, "(Ljava/lang/String;I)Ljava/lang/String;", null, null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitLineNumber(7, label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "getBytes", "()[B", false);
        methodVisitor.visitVarInsn(ASTORE, 2);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitLineNumber(8, label1);
        methodVisitor.visitVarInsn(ILOAD, 1);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "toString", "(I)Ljava/lang/String;", false);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "getBytes", "()[B", false);
        methodVisitor.visitVarInsn(ASTORE, 3);
        Label label2 = new Label();
        methodVisitor.visitLabel(label2);
        methodVisitor.visitLineNumber(10, label2);
        methodVisitor.visitInsn(ICONST_0);
        methodVisitor.visitVarInsn(ISTORE, 4);
        Label label3 = new Label();
        methodVisitor.visitLabel(label3);
        methodVisitor.visitFrame(Opcodes.F_NEW, 5, new Object[]{"java/lang/String", Opcodes.INTEGER, "[B", "[B", Opcodes.INTEGER}, 0, new Object[]{});
        methodVisitor.visitVarInsn(ILOAD, 4);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitInsn(ARRAYLENGTH);
        Label label4 = new Label();
        methodVisitor.visitJumpInsn(IF_ICMPGE, label4);
        Label label5 = new Label();
        methodVisitor.visitLabel(label5);
        methodVisitor.visitLineNumber(11, label5);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitVarInsn(ILOAD, 4);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitVarInsn(ILOAD, 4);
        methodVisitor.visitInsn(BALOAD);
        methodVisitor.visitVarInsn(ALOAD, 3);
        methodVisitor.visitVarInsn(ILOAD, 4);
        methodVisitor.visitVarInsn(ALOAD, 3);
        methodVisitor.visitInsn(ARRAYLENGTH);
        methodVisitor.visitInsn(IREM);
        methodVisitor.visitInsn(BALOAD);
        methodVisitor.visitInsn(IXOR);
        methodVisitor.visitInsn(I2B);
        methodVisitor.visitInsn(BASTORE);
        Label label6 = new Label();
        methodVisitor.visitLabel(label6);
        methodVisitor.visitLineNumber(10, label6);
        methodVisitor.visitIincInsn(4, 1);
        methodVisitor.visitJumpInsn(GOTO, label3);
        methodVisitor.visitLabel(label4);
        methodVisitor.visitLineNumber(14, label4);
        methodVisitor.visitFrame(Opcodes.F_NEW, 4, new Object[]{"java/lang/String", Opcodes.INTEGER, "[B", "[B"}, 0, new Object[]{});
        methodVisitor.visitTypeInsn(NEW, "java/lang/String");
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>", "([B)V", false);
        methodVisitor.visitInsn(ARETURN);
        Label label7 = new Label();
        methodVisitor.visitLabel(label7);
        methodVisitor.visitLocalVariable("i", "I", null, label3, label4, 4);
        methodVisitor.visitLocalVariable("string", "Ljava/lang/String;", null, label0, label7, 0);
        methodVisitor.visitLocalVariable("key", "I", null, label0, label7, 1);
        methodVisitor.visitLocalVariable("encrypted", "[B", null, label1, label7, 2);
        methodVisitor.visitLocalVariable("modulo", "[B", null, label2, label7, 3);
        methodVisitor.visitMaxs(6, 5);
        methodVisitor.visitEnd();
    }
}
