package dev.skidfuscator.obfuscator.transform.impl.string.generator;

import dev.skidfuscator.obfuscator.event.EventBus;
import dev.skidfuscator.obfuscator.event.impl.transform.method.InitMethodTransformEvent;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import org.mapleir.asm.FieldNode;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;

public class BytesEncryptionGenerator implements EncryptionGenerator {
    public static final String METHOD_NAME = "thisIsAInsaneEncryptionMethod";
    private final Integer[] keys;

    public BytesEncryptionGenerator(Integer[] keys) {
        this.keys = keys;
    }

    @Override
    public byte[] encrypt(String input, int key) {
        final byte[] encrypted = input.getBytes(StandardCharsets.UTF_16);

        // Super simple converting our integer to string, and getting bytes.
        final byte[] keyBytes = Integer.toString(key).getBytes();

        // Super simple XOR
        for (int i = 0; i < encrypted.length; i++) {
            encrypted[i] ^= keyBytes[i % keyBytes.length];
            encrypted[i] ^= keys[i % keys.length];
        }

        // Base64 encode it for testing
        return encrypted;
    }

    @Override
    public String decrypt(byte[] input, int key) {
        // Super simple converting our integer to string, and getting bytes.
        final byte[] keyBytes = Integer.toString(key).getBytes();

        // Super simple XOR
        for (int i = 0; i < input.length; i++) {
            input[i] ^= keyBytes[i % keyBytes.length];
            input[i] ^= keys[i % keys.length];
        }

        // Base64 encode it for testing
        return new String(input, StandardCharsets.UTF_16);
    }

    @Override
    public void visit(final SkidClassNode node, String name) {
        // TODO: Fix the retardness and make it so that all
        //       generated methods can be properly genned
        final SkidMethodNode skidMethodNode = node.createMethod()
                .access(ACC_PRIVATE | ACC_STATIC)
                .name(name)
                .desc("([BI)Ljava/lang/String;")
                .phantom(true)
                .build();
        final MethodVisitor methodVisitor = skidMethodNode.node;

        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitLineNumber(16, label0);
        methodVisitor.visitVarInsn(ILOAD, 1);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "toString", "(I)Ljava/lang/String;", false);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "getBytes", "()[B", false);
        methodVisitor.visitVarInsn(ASTORE, 2);

        // Keys
        methodVisitor.visitIntInsn(BIPUSH, keys.length);
        methodVisitor.visitIntInsn(NEWARRAY, T_BYTE);

        for (int i = 0; i < keys.length; i++) {
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitIntInsn(BIPUSH, i);
            methodVisitor.visitIntInsn(BIPUSH, keys[i]);
            methodVisitor.visitInsn(BASTORE);
        }

        methodVisitor.visitVarInsn(ASTORE, 4);

        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitLineNumber(19, label1);
        methodVisitor.visitInsn(ICONST_0);
        methodVisitor.visitVarInsn(ISTORE, 3);
        Label label2 = new Label();
        methodVisitor.visitLabel(label2);
        methodVisitor.visitFrame(Opcodes.F_APPEND, 2, new Object[]{"[B", Opcodes.INTEGER}, 0, null);
        methodVisitor.visitVarInsn(ILOAD, 3);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitInsn(ARRAYLENGTH);
        Label label3 = new Label();
        methodVisitor.visitJumpInsn(IF_ICMPGE, label3);
        Label label4 = new Label();
        methodVisitor.visitLabel(label4);
        methodVisitor.visitLineNumber(20, label4);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitVarInsn(ILOAD, 3);
        methodVisitor.visitInsn(DUP2);
        methodVisitor.visitInsn(BALOAD);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitVarInsn(ILOAD, 3);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitInsn(ARRAYLENGTH);
        methodVisitor.visitInsn(IREM);
        methodVisitor.visitInsn(BALOAD);
        methodVisitor.visitInsn(IXOR);
        methodVisitor.visitInsn(I2B);
        methodVisitor.visitInsn(BASTORE);
        Label label5 = new Label();
        methodVisitor.visitLabel(label5);
        methodVisitor.visitLineNumber(21, label5);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitVarInsn(ILOAD, 3);
        methodVisitor.visitInsn(DUP2);
        methodVisitor.visitInsn(BALOAD);
        methodVisitor.visitVarInsn(ALOAD, 4);
        methodVisitor.visitVarInsn(ILOAD, 3);
        methodVisitor.visitVarInsn(ALOAD, 4);
        methodVisitor.visitInsn(ARRAYLENGTH);
        methodVisitor.visitInsn(IREM);
        methodVisitor.visitInsn(BALOAD);
        methodVisitor.visitInsn(IXOR);
        methodVisitor.visitInsn(I2B);
        methodVisitor.visitInsn(BASTORE);
        Label label6 = new Label();
        methodVisitor.visitLabel(label6);
        methodVisitor.visitLineNumber(19, label6);
        methodVisitor.visitIincInsn(3, 1);
        methodVisitor.visitJumpInsn(GOTO, label2);
        methodVisitor.visitLabel(label3);
        methodVisitor.visitLineNumber(25, label3);
        methodVisitor.visitFrame(Opcodes.F_CHOP, 1, null, 0, null);
        methodVisitor.visitTypeInsn(NEW, "java/lang/String");
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitFieldInsn(GETSTATIC, "java/nio/charset/StandardCharsets", "UTF_16", "Ljava/nio/charset/Charset;");
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>", "([BLjava/nio/charset/Charset;)V", false);
        methodVisitor.visitInsn(ARETURN);
        Label label7 = new Label();
        methodVisitor.visitLabel(label7);
        methodVisitor.visitLocalVariable("i", "I", null, label2, label3, 3);
        methodVisitor.visitLocalVariable("input", "[B", null, label0, label7, 0);
        methodVisitor.visitLocalVariable("key", "I", null, label0, label7, 1);
        methodVisitor.visitLocalVariable("keyBytes", "[B", null, label1, label7, 2);
        methodVisitor.visitLocalVariable("keys", "[B", null, label0, label7, 3);
        methodVisitor.visitMaxs(6, 5);
        methodVisitor.visitEnd();

        // /!\ Absolutely critical line
        skidMethodNode.recomputeCfg();
        EventBus.call(
                new InitMethodTransformEvent(skidMethodNode.getSkidfuscator(), skidMethodNode)
        );
    }
}
