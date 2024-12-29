package dev.skidfuscator.obfuscator.transform.impl.string.generator.guard;

import dev.skidfuscator.obfuscator.event.EventBus;
import dev.skidfuscator.obfuscator.event.impl.transform.method.InitMethodTransformEvent;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.skidasm.SkidFieldNode;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.transform.impl.string.generator.EncryptionGenerator;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.nio.charset.StandardCharsets;

import static org.objectweb.asm.Opcodes.*;

public class StringPoolerEncryptionGenerator implements EncryptionGenerator {
    public static final String METHOD_NAME = "thisIsAInsaneEncryptionMethod";
    private final Integer[] keys;
    private SkidFieldNode storageField;

    public StringPoolerEncryptionGenerator(Integer[] keys) {
        this.keys = keys;
    }

    @Override
    public byte[] encrypt(String input, int key) {
        final byte[] byteBuffer = input.getBytes(StandardCharsets.UTF_16BE);
        final byte[] encryptedByteBuffer = new byte[8];

        // Encode location of the buffer
        final int size = ((String) storageField.node.value).length();
        encryptedByteBuffer[4] = (byte) (size >> 24);
        encryptedByteBuffer[5] = (byte) (size >> 16);
        encryptedByteBuffer[6] = (byte) (size >> 8);
        encryptedByteBuffer[7] = (byte) size;

        // Super simple converting our integer to string, and getting bytes.
        final byte[] keyBytes = Integer.toString(key).getBytes();

        // Super simple XOR
        for (int i = 0; i < byteBuffer.length; i++) {
            byteBuffer[i] ^= keyBytes[i % keyBytes.length];
            byteBuffer[i] ^= keys[i % keys.length];
        }

        final String values = new String(byteBuffer, StandardCharsets.UTF_16BE);

        final int length = values.length();
        // Encode length of the buffer
        encryptedByteBuffer[0] = (byte) (length >> 24);
        encryptedByteBuffer[1] = (byte) (length >> 16);
        encryptedByteBuffer[2] = (byte) (length >> 8);
        encryptedByteBuffer[3] = (byte) length;

        storageField.node.value = ((String) storageField.node.value) + values;

        // Base64 encode it for testing
        return encryptedByteBuffer;
    }

    @Override
    public String decrypt(byte[] index, int key) {
        // decrypt the length from the input buffer
        final int length = (index[0] & 0xFF << 24) | (index[1] & 0xFF << 16) | (index[2] & 0xFF << 8) | index[3] & 0xFF;
        final int size = (index[4] & 0xFF << 24) | (index[5] & 0xFF << 16) | (index[6] & 0xFF << 8) | index[7] & 0xFF;

        // Super simple converting our integer to string, and getting bytes.
        final byte[] keyBytes = Integer.toString(key).getBytes();
        final byte[] input = ((String) storageField.node.value).substring(size, size + length).getBytes(StandardCharsets.UTF_16BE);

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
        this.storageField = node.createField()
                .access(node.isInterface() ? ACC_PUBLIC | ACC_FINAL | ACC_STATIC : ACC_PRIVATE | ACC_STATIC)
                .name("thisIsAInsaneEncryptionField")
                .desc("Ljava/lang/String;")
                .value("")
                .build();
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
        methodVisitor.visitLineNumber(39, label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitInsn(ICONST_0);
        methodVisitor.visitInsn(BALOAD);
        methodVisitor.visitIntInsn(SIPUSH, 255);
        methodVisitor.visitInsn(IAND);
        methodVisitor.visitIntInsn(BIPUSH, 24);
        methodVisitor.visitInsn(ISHL);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitInsn(ICONST_1);
        methodVisitor.visitInsn(BALOAD);
        methodVisitor.visitIntInsn(SIPUSH, 255);
        methodVisitor.visitInsn(IAND);
        methodVisitor.visitIntInsn(BIPUSH, 16);
        methodVisitor.visitInsn(ISHL);
        methodVisitor.visitInsn(IOR);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitInsn(ICONST_2);
        methodVisitor.visitInsn(BALOAD);
        methodVisitor.visitIntInsn(SIPUSH, 255);
        methodVisitor.visitInsn(IAND);
        methodVisitor.visitIntInsn(BIPUSH, 8);
        methodVisitor.visitInsn(ISHL);
        methodVisitor.visitInsn(IOR);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitInsn(ICONST_3);
        methodVisitor.visitInsn(BALOAD);
        methodVisitor.visitIntInsn(SIPUSH, 255);
        methodVisitor.visitInsn(IAND);
        methodVisitor.visitInsn(IOR);
        methodVisitor.visitVarInsn(ISTORE, 2);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitLineNumber(40, label1);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitInsn(ICONST_4);
        methodVisitor.visitInsn(BALOAD);
        methodVisitor.visitIntInsn(SIPUSH, 255);
        methodVisitor.visitInsn(IAND);
        methodVisitor.visitIntInsn(BIPUSH, 24);
        methodVisitor.visitInsn(ISHL);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitInsn(ICONST_5);
        methodVisitor.visitInsn(BALOAD);
        methodVisitor.visitIntInsn(SIPUSH, 255);
        methodVisitor.visitInsn(IAND);
        methodVisitor.visitIntInsn(BIPUSH, 16);
        methodVisitor.visitInsn(ISHL);
        methodVisitor.visitInsn(IOR);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitIntInsn(BIPUSH, 6);
        methodVisitor.visitInsn(BALOAD);
        methodVisitor.visitIntInsn(SIPUSH, 255);
        methodVisitor.visitInsn(IAND);
        methodVisitor.visitIntInsn(BIPUSH, 8);
        methodVisitor.visitInsn(ISHL);
        methodVisitor.visitInsn(IOR);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitIntInsn(BIPUSH, 7);
        methodVisitor.visitInsn(BALOAD);
        methodVisitor.visitIntInsn(SIPUSH, 255);
        methodVisitor.visitInsn(IAND);
        methodVisitor.visitInsn(IOR);
        methodVisitor.visitVarInsn(ISTORE, 3);
        Label label2 = new Label();
        methodVisitor.visitLabel(label2);
        methodVisitor.visitLineNumber(43, label2);
        methodVisitor.visitVarInsn(ILOAD, 1);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "toString", "(I)Ljava/lang/String;", false);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "getBytes", "()[B", false);
        methodVisitor.visitVarInsn(ASTORE, 4);
        Label label3 = new Label();
        methodVisitor.visitLabel(label3);
        methodVisitor.visitLineNumber(39, label3);
        // Changed
        methodVisitor.visitFieldInsn(GETSTATIC, storageField.getOwner(), storageField.getName(), "Ljava/lang/String;");
        methodVisitor.visitVarInsn(ILOAD, 3);
        methodVisitor.visitVarInsn(ILOAD, 3);
        methodVisitor.visitVarInsn(ILOAD, 2);
        methodVisitor.visitInsn(IADD);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "substring", "(II)Ljava/lang/String;", false);
        methodVisitor.visitFieldInsn(GETSTATIC, "java/nio/charset/StandardCharsets", "UTF_16BE", "Ljava/nio/charset/Charset;");
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "getBytes", "(Ljava/nio/charset/Charset;)[B", false);
        methodVisitor.visitVarInsn(ASTORE, 5);
        Label label4 = new Label();
        methodVisitor.visitLabel(label4);
        methodVisitor.visitLineNumber(40, label4);

        // Keys
        methodVisitor.visitIntInsn(BIPUSH, keys.length);
        methodVisitor.visitIntInsn(NEWARRAY, T_BYTE);

        for (int i = 0; i < keys.length; i++) {
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitIntInsn(BIPUSH, i);
            methodVisitor.visitIntInsn(BIPUSH, keys[i]);
            methodVisitor.visitInsn(BASTORE);
        }

        methodVisitor.visitVarInsn(ASTORE, 6);
        Label label5 = new Label();
        methodVisitor.visitLabel(label5);
        methodVisitor.visitLineNumber(42, label5);
        methodVisitor.visitInsn(ICONST_0);
        methodVisitor.visitVarInsn(ISTORE, 7);
        Label label6 = new Label();
        methodVisitor.visitLabel(label6);
        methodVisitor.visitFrame(Opcodes.F_FULL, 8, new Object[]{"[B", Opcodes.INTEGER, Opcodes.INTEGER, Opcodes.INTEGER, "[B", "[B", "[B", Opcodes.INTEGER}, 0, new Object[]{});
        methodVisitor.visitVarInsn(ILOAD, 7);
        methodVisitor.visitVarInsn(ALOAD, 5);
        methodVisitor.visitInsn(ARRAYLENGTH);
        Label label7 = new Label();
        methodVisitor.visitJumpInsn(IF_ICMPGE, label7);
        Label label8 = new Label();
        methodVisitor.visitLabel(label8);
        methodVisitor.visitLineNumber(43, label8);
        methodVisitor.visitVarInsn(ALOAD, 5);
        methodVisitor.visitVarInsn(ILOAD, 7);
        methodVisitor.visitInsn(DUP2);
        methodVisitor.visitInsn(BALOAD);
        methodVisitor.visitVarInsn(ALOAD, 4);
        methodVisitor.visitVarInsn(ILOAD, 7);
        methodVisitor.visitVarInsn(ALOAD, 4);
        methodVisitor.visitInsn(ARRAYLENGTH);
        methodVisitor.visitInsn(IREM);
        methodVisitor.visitInsn(BALOAD);
        methodVisitor.visitInsn(IXOR);
        methodVisitor.visitInsn(I2B);
        methodVisitor.visitInsn(BASTORE);
        Label label9 = new Label();
        methodVisitor.visitLabel(label9);
        methodVisitor.visitLineNumber(44, label9);
        methodVisitor.visitVarInsn(ALOAD, 5);
        methodVisitor.visitVarInsn(ILOAD, 7);
        methodVisitor.visitInsn(DUP2);
        methodVisitor.visitInsn(BALOAD);
        methodVisitor.visitVarInsn(ALOAD, 6);
        methodVisitor.visitVarInsn(ILOAD, 7);
        methodVisitor.visitVarInsn(ALOAD, 6);
        methodVisitor.visitInsn(ARRAYLENGTH);
        methodVisitor.visitInsn(IREM);
        methodVisitor.visitInsn(BALOAD);
        methodVisitor.visitInsn(IXOR);
        methodVisitor.visitInsn(I2B);
        methodVisitor.visitInsn(BASTORE);
        Label label10 = new Label();
        methodVisitor.visitLabel(label10);
        methodVisitor.visitLineNumber(42, label10);
        methodVisitor.visitIincInsn(7, 1);
        methodVisitor.visitJumpInsn(GOTO, label6);
        methodVisitor.visitLabel(label7);
        methodVisitor.visitLineNumber(48, label7);
        methodVisitor.visitFrame(Opcodes.F_CHOP, 1, null, 0, null);
        methodVisitor.visitTypeInsn(NEW, "java/lang/String");
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitVarInsn(ALOAD, 5);
        methodVisitor.visitFieldInsn(GETSTATIC, "java/nio/charset/StandardCharsets", "UTF_16", "Ljava/nio/charset/Charset;");
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>", "([BLjava/nio/charset/Charset;)V", false);
        methodVisitor.visitInsn(ARETURN);
        Label label11 = new Label();
        methodVisitor.visitLabel(label11);
        methodVisitor.visitLocalVariable("i", "I", null, label6, label7, 7);
        methodVisitor.visitLocalVariable("index", "[B", null, label0, label11, 0);
        methodVisitor.visitLocalVariable("key", "I", null, label0, label11, 1);
        methodVisitor.visitLocalVariable("length", "I", null, label1, label11, 2);
        methodVisitor.visitLocalVariable("size", "I", null, label2, label11, 3);
        methodVisitor.visitLocalVariable("keyBytes", "[B", null, label3, label11, 4);
        methodVisitor.visitLocalVariable("input", "[B", null, label4, label11, 5);
        methodVisitor.visitLocalVariable("keys", "[B", null, label5, label11, 6);
        methodVisitor.visitMaxs(6, 8);
        methodVisitor.visitEnd();

        // /!\ Absolutely critical line
        skidMethodNode.recomputeCfg();
        EventBus.call(
                new InitMethodTransformEvent(skidMethodNode.getSkidfuscator(), skidMethodNode)
        );
    }
}
