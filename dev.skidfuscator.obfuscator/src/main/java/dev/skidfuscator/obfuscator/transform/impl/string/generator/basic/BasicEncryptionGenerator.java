package dev.skidfuscator.obfuscator.transform.impl.string.generator.basic;

import dev.skidfuscator.obfuscator.event.EventBus;
import dev.skidfuscator.obfuscator.event.impl.transform.method.InitMethodTransformEvent;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.transform.impl.string.generator.EncryptionGenerator;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.objectweb.asm.Opcodes.*;

public class BasicEncryptionGenerator implements EncryptionGenerator {
    public static final String METHOD_NAME = "thisIsAInsaneEncryptionMethod";
    private final Integer[] keys;

    public BasicEncryptionGenerator(Integer[] keys) {
        this.keys = keys;
    }

    @Override
    public byte[] encrypt(String input, int key) {
        final byte[] encrypted = input.getBytes();

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
                .desc("(Ljava/lang/String;I)Ljava/lang/String;")
                .phantom(true)
                .build();
        final MethodVisitor methodVisitor = skidMethodNode.node;

        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitLineNumber(43, label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "getBytes", "()[B", false);
        methodVisitor.visitVarInsn(ASTORE, 2);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitLineNumber(36, label1);
        methodVisitor.visitVarInsn(ILOAD, 1);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "toString", "(I)Ljava/lang/String;", false);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "getBytes", "()[B", false);
        methodVisitor.visitVarInsn(ASTORE, 3);
        Label label2 = new Label();
        methodVisitor.visitLabel(label2);
        methodVisitor.visitLineNumber(37, label2);
        methodVisitor.visitIntInsn(BIPUSH, keys.length);
        methodVisitor.visitIntInsn(NEWARRAY, T_BYTE);

        for (int i = 0; i < keys.length; i++) {
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitIntInsn(BIPUSH, i);
            methodVisitor.visitIntInsn(BIPUSH, keys[i]);
            methodVisitor.visitInsn(BASTORE);
        }

        methodVisitor.visitVarInsn(ASTORE, 4);
        Label label3 = new Label();
        methodVisitor.visitLabel(label3);
        methodVisitor.visitLineNumber(40, label3);
        methodVisitor.visitInsn(ICONST_0);
        methodVisitor.visitVarInsn(ISTORE, 5);
        Label label4 = new Label();
        methodVisitor.visitLabel(label4);
        methodVisitor.visitFrame(Opcodes.F_FULL, 6, new Object[]{"java/lang/String", Opcodes.INTEGER, "[B", "[B", "[B", Opcodes.INTEGER}, 0, new Object[]{});
        methodVisitor.visitVarInsn(ILOAD, 5);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitInsn(ARRAYLENGTH);
        Label label5 = new Label();
        methodVisitor.visitJumpInsn(IF_ICMPGE, label5);
        Label label6 = new Label();
        methodVisitor.visitLabel(label6);
        methodVisitor.visitLineNumber(41, label6);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitVarInsn(ILOAD, 5);
        methodVisitor.visitInsn(DUP2);
        methodVisitor.visitInsn(BALOAD);
        methodVisitor.visitVarInsn(ALOAD, 3);
        methodVisitor.visitVarInsn(ILOAD, 5);
        methodVisitor.visitVarInsn(ALOAD, 3);
        methodVisitor.visitInsn(ARRAYLENGTH);
        methodVisitor.visitInsn(IREM);
        methodVisitor.visitInsn(BALOAD);
        methodVisitor.visitInsn(IXOR);
        methodVisitor.visitInsn(I2B);
        methodVisitor.visitInsn(BASTORE);
        Label label7 = new Label();
        methodVisitor.visitLabel(label7);
        methodVisitor.visitLineNumber(42, label7);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitVarInsn(ILOAD, 5);
        methodVisitor.visitInsn(DUP2);
        methodVisitor.visitInsn(BALOAD);
        methodVisitor.visitVarInsn(ALOAD, 4);
        methodVisitor.visitVarInsn(ILOAD, 5);
        methodVisitor.visitVarInsn(ALOAD, 4);
        methodVisitor.visitInsn(ARRAYLENGTH);
        methodVisitor.visitInsn(IREM);
        methodVisitor.visitInsn(BALOAD);
        methodVisitor.visitInsn(IXOR);
        methodVisitor.visitInsn(I2B);
        methodVisitor.visitInsn(BASTORE);
        Label label8 = new Label();
        methodVisitor.visitLabel(label8);
        methodVisitor.visitLineNumber(40, label8);
        methodVisitor.visitIincInsn(5, 1);
        methodVisitor.visitJumpInsn(GOTO, label4);
        methodVisitor.visitLabel(label5);
        methodVisitor.visitLineNumber(46, label5);
        methodVisitor.visitFrame(Opcodes.F_CHOP, 1, null, 0, null);
        methodVisitor.visitTypeInsn(NEW, "java/lang/String");
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitFieldInsn(GETSTATIC, "java/nio/charset/StandardCharsets", "UTF_8", "Ljava/nio/charset/Charset;");
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>", "([BLjava/nio/charset/Charset;)V", false);
        methodVisitor.visitInsn(ARETURN);
        Label label9 = new Label();
        methodVisitor.visitLabel(label9);
        methodVisitor.visitLocalVariable("i", "I", null, label4, label5, 5);
        methodVisitor.visitLocalVariable("input", "Ljava/lang/String;", null, label0, label9, 0);
        methodVisitor.visitLocalVariable("key", "I", null, label0, label9, 1);
        methodVisitor.visitLocalVariable("decrypted", "[B", null, label1, label9, 2);
        methodVisitor.visitLocalVariable("keyBytes", "[B", null, label2, label9, 3);
        methodVisitor.visitLocalVariable("keys", "[B", null, label3, label9, 4);
        methodVisitor.visitMaxs(6, 6);
        methodVisitor.visitEnd();

        // /!\ Absolutely critical line
        skidMethodNode.recomputeCfg();
        EventBus.call(
                new InitMethodTransformEvent(skidMethodNode.getSkidfuscator(), skidMethodNode)
        );
    }
}
