package dev.skidfuscator.obfuscator.transform.impl.string.generator.algo;

import dev.skidfuscator.obfuscator.event.EventBus;
import dev.skidfuscator.obfuscator.event.impl.transform.method.InitMethodTransformEvent;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.transform.impl.string.generator.EncryptionGenerator;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.nio.charset.StandardCharsets;

import static org.objectweb.asm.Opcodes.*;

public class CaesarEncryptionGenerator implements EncryptionGenerator {
    private final Integer[] keys;

    public CaesarEncryptionGenerator(Integer[] keys) {
        this.keys = keys;
    }

    @Override
    public byte[] encrypt(String input, int key) {
        final byte[] saved = input.getBytes(StandardCharsets.UTF_16);
        final byte[] encrypted = new byte[saved.length];

        // Super simple XOR
        for (int i = 0; i < saved.length; i++) {
            encrypted[(i + key) % saved.length] = saved[i];
        }

        for (int i = 0; i < saved.length; i++) {
            encrypted[i] ^= keys[i % keys.length];
        }

        // Base64 encode it for testing
        return encrypted;
    }

    @Override
    public String decrypt(byte[] input, int key) {
        // Super simple converting our integer to string, and getting bytes.
        final byte[] decrypted = new byte[input.length];

        for (int i = 0; i < input.length; i++) {
            input[i] ^= keys[i % keys.length];
        }

        // Super simple XOR
        for (int i = 0; i < input.length; i++) {
            decrypted[(i + (input.length - (key % input.length))) % input.length] = input[i];
        }

        // Base64 encode it for testing
        return new String(decrypted, StandardCharsets.UTF_16);
    }

    public static String decryptStatic(byte[] input, int key) {
        // Super simple converting our integer to string, and getting bytes.
        final byte[] decrypted = new byte[input.length];
        final int[] keys = {1728, 733, 772, 3772};

        for (int i = 0; i < input.length; i++) {
            input[i] ^= keys[i % keys.length];
        }

        // Super simple XOR
        for (int i = 0; i < input.length; i++) {
            decrypted[(i + (input.length - (key % input.length))) % input.length] = input[i];
        }

        // Base64 encode it for testing
        return new String(decrypted, StandardCharsets.UTF_16);
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
        methodVisitor.visitLineNumber(61, label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitInsn(ARRAYLENGTH);
        methodVisitor.visitIntInsn(NEWARRAY, T_BYTE);
        methodVisitor.visitVarInsn(ASTORE, 2);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitLineNumber(62, label1);
        methodVisitor.visitInsn(ICONST_4);
        methodVisitor.visitIntInsn(NEWARRAY, T_INT);

        for (int i = 0; i < keys.length; i++) {
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitIntInsn(BIPUSH, i);
            methodVisitor.visitIntInsn(BIPUSH, keys[i]);
            methodVisitor.visitInsn(IASTORE);
        }

        methodVisitor.visitVarInsn(ASTORE, 3);
        Label label2 = new Label();
        methodVisitor.visitLabel(label2);
        methodVisitor.visitLineNumber(64, label2);
        methodVisitor.visitInsn(ICONST_0);
        methodVisitor.visitVarInsn(ISTORE, 4);
        Label label3 = new Label();
        methodVisitor.visitLabel(label3);
        methodVisitor.visitFrame(Opcodes.F_APPEND, 3, new Object[]{"[B", "[I", Opcodes.INTEGER}, 0, null);
        methodVisitor.visitVarInsn(ILOAD, 4);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitInsn(ARRAYLENGTH);
        Label label4 = new Label();
        methodVisitor.visitJumpInsn(IF_ICMPGE, label4);
        Label label5 = new Label();
        methodVisitor.visitLabel(label5);
        methodVisitor.visitLineNumber(65, label5);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitVarInsn(ILOAD, 4);
        methodVisitor.visitInsn(DUP2);
        methodVisitor.visitInsn(BALOAD);
        methodVisitor.visitVarInsn(ALOAD, 3);
        methodVisitor.visitVarInsn(ILOAD, 4);
        methodVisitor.visitVarInsn(ALOAD, 3);
        methodVisitor.visitInsn(ARRAYLENGTH);
        methodVisitor.visitInsn(IREM);
        methodVisitor.visitInsn(IALOAD);
        methodVisitor.visitInsn(IXOR);
        methodVisitor.visitInsn(I2B);
        methodVisitor.visitInsn(BASTORE);
        Label label6 = new Label();
        methodVisitor.visitLabel(label6);
        methodVisitor.visitLineNumber(64, label6);
        methodVisitor.visitIincInsn(4, 1);
        methodVisitor.visitJumpInsn(GOTO, label3);
        methodVisitor.visitLabel(label4);
        methodVisitor.visitLineNumber(69, label4);
        methodVisitor.visitFrame(Opcodes.F_CHOP, 1, null, 0, null);
        methodVisitor.visitInsn(ICONST_0);
        methodVisitor.visitVarInsn(ISTORE, 4);
        Label label7 = new Label();
        methodVisitor.visitLabel(label7);
        methodVisitor.visitFrame(Opcodes.F_APPEND, 1, new Object[]{Opcodes.INTEGER}, 0, null);
        methodVisitor.visitVarInsn(ILOAD, 4);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitInsn(ARRAYLENGTH);
        Label label8 = new Label();
        methodVisitor.visitJumpInsn(IF_ICMPGE, label8);
        Label label9 = new Label();
        methodVisitor.visitLabel(label9);
        methodVisitor.visitLineNumber(70, label9);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitVarInsn(ILOAD, 4);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitInsn(ARRAYLENGTH);
        methodVisitor.visitVarInsn(ILOAD, 1);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitInsn(ARRAYLENGTH);
        methodVisitor.visitInsn(IREM);
        methodVisitor.visitInsn(ISUB);
        methodVisitor.visitInsn(IADD);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitInsn(ARRAYLENGTH);
        methodVisitor.visitInsn(IREM);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitVarInsn(ILOAD, 4);
        methodVisitor.visitInsn(BALOAD);
        methodVisitor.visitInsn(BASTORE);
        Label label10 = new Label();
        methodVisitor.visitLabel(label10);
        methodVisitor.visitLineNumber(69, label10);
        methodVisitor.visitIincInsn(4, 1);
        methodVisitor.visitJumpInsn(GOTO, label7);
        methodVisitor.visitLabel(label8);
        methodVisitor.visitLineNumber(74, label8);
        methodVisitor.visitFrame(Opcodes.F_CHOP, 1, null, 0, null);
        methodVisitor.visitTypeInsn(NEW, "java/lang/String");
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitFieldInsn(GETSTATIC, "java/nio/charset/StandardCharsets", "UTF_16", "Ljava/nio/charset/Charset;");
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>", "([BLjava/nio/charset/Charset;)V", false);
        methodVisitor.visitInsn(ARETURN);
        Label label11 = new Label();
        methodVisitor.visitLabel(label11);
        methodVisitor.visitMaxs(6, 5);
        methodVisitor.visitEnd();

        // /!\ Absolutely critical line
        skidMethodNode.recomputeCfg();
        EventBus.call(
                new InitMethodTransformEvent(skidMethodNode.getSkidfuscator(), skidMethodNode)
        );
    }
}
