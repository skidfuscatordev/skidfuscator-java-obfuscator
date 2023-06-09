package dev.skidfuscator.obfuscator.transform.impl.string.generator.algo;

import dev.skidfuscator.obfuscator.event.EventBus;
import dev.skidfuscator.obfuscator.event.impl.transform.method.InitMethodTransformEvent;
import dev.skidfuscator.obfuscator.skidasm.SkidClassNode;
import dev.skidfuscator.obfuscator.skidasm.SkidMethodNode;
import dev.skidfuscator.obfuscator.transform.impl.string.generator.EncryptionGenerator;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import static org.objectweb.asm.Opcodes.*;

public class AESEncryptionGenerator implements EncryptionGenerator {
    private final String iv;

    public AESEncryptionGenerator(String iv) {
        assert iv.length() == 16 : "AES IV must be 16 bytes long";
        this.iv = iv;
    }

    @Override
    public byte[] encrypt(String input, int key) {
        try {
            final IvParameterSpec iv = new IvParameterSpec(this.iv.getBytes(StandardCharsets.UTF_8));
            final Random random = new Random(key);
            final byte[] secureRandomKeyBytes = new byte[256 / 8];

            random.nextBytes(secureRandomKeyBytes);
            final SecretKeySpec skeySpec = new SecretKeySpec(
                    secureRandomKeyBytes,
                    "AES"
            );

            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            return cipher.doFinal(input.getBytes(StandardCharsets.UTF_16));
        } catch (Throwable ex) {
            throw new IllegalStateException("Failed to encrypt", ex);
        }
    }

    @Override
    public String decrypt(byte[] input, int key) {
        try {
            final IvParameterSpec iv = new IvParameterSpec(this.iv.getBytes(StandardCharsets.UTF_8));
            final Random random = new Random(key);
            final byte[] secureRandomKeyBytes = new byte[256 / 8];
            random.nextBytes(secureRandomKeyBytes);

            final SecretKeySpec skeySpec = new SecretKeySpec(secureRandomKeyBytes, "AES");

            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            byte[] original = cipher.doFinal(input);

            return new String(original, StandardCharsets.UTF_16);
        } catch (Throwable ex) {
            throw new IllegalStateException("Failed to decrypt", ex);
        }
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
        Label label1 = new Label();
        Label label2 = new Label();
        methodVisitor.visitTryCatchBlock(label0, label1, label2, "java/lang/Throwable");
        methodVisitor.visitLabel(label0);
        methodVisitor.visitLineNumber(52, label0);
        methodVisitor.visitTypeInsn(NEW, "javax/crypto/spec/IvParameterSpec");
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitLdcInsn(iv);
        methodVisitor.visitFieldInsn(GETSTATIC, "java/nio/charset/StandardCharsets", "UTF_8", "Ljava/nio/charset/Charset;");
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "getBytes", "(Ljava/nio/charset/Charset;)[B", false);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "javax/crypto/spec/IvParameterSpec", "<init>", "([B)V", false);
        methodVisitor.visitVarInsn(ASTORE, 3);
        Label label3 = new Label();
        methodVisitor.visitLabel(label3);
        methodVisitor.visitLineNumber(53, label3);
        methodVisitor.visitTypeInsn(NEW, "java/util/Random");
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitVarInsn(ILOAD, 1);
        methodVisitor.visitInsn(I2L);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/util/Random", "<init>", "(J)V", false);
        methodVisitor.visitVarInsn(ASTORE, 4);
        Label label4 = new Label();
        methodVisitor.visitLabel(label4);
        methodVisitor.visitLineNumber(54, label4);
        methodVisitor.visitIntInsn(BIPUSH, 32);
        methodVisitor.visitIntInsn(NEWARRAY, T_BYTE);
        methodVisitor.visitVarInsn(ASTORE, 5);
        Label label5 = new Label();
        methodVisitor.visitLabel(label5);
        methodVisitor.visitLineNumber(55, label5);
        methodVisitor.visitVarInsn(ALOAD, 4);
        methodVisitor.visitVarInsn(ALOAD, 5);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/util/Random", "nextBytes", "([B)V", false);
        Label label6 = new Label();
        methodVisitor.visitLabel(label6);
        methodVisitor.visitLineNumber(57, label6);
        methodVisitor.visitTypeInsn(NEW, "javax/crypto/spec/SecretKeySpec");
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitVarInsn(ALOAD, 5);
        methodVisitor.visitLdcInsn("AES");
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "javax/crypto/spec/SecretKeySpec", "<init>", "([BLjava/lang/String;)V", false);
        methodVisitor.visitVarInsn(ASTORE, 6);
        Label label7 = new Label();
        methodVisitor.visitLabel(label7);
        methodVisitor.visitLineNumber(59, label7);
        methodVisitor.visitLdcInsn("AES/CBC/PKCS5PADDING");
        methodVisitor.visitMethodInsn(INVOKESTATIC, "javax/crypto/Cipher", "getInstance", "(Ljava/lang/String;)Ljavax/crypto/Cipher;", false);
        methodVisitor.visitVarInsn(ASTORE, 7);
        Label label8 = new Label();
        methodVisitor.visitLabel(label8);
        methodVisitor.visitLineNumber(60, label8);
        methodVisitor.visitVarInsn(ALOAD, 7);
        methodVisitor.visitInsn(ICONST_2);
        methodVisitor.visitVarInsn(ALOAD, 6);
        methodVisitor.visitVarInsn(ALOAD, 3);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "javax/crypto/Cipher", "init", "(ILjava/security/Key;Ljava/security/spec/AlgorithmParameterSpec;)V", false);
        Label label9 = new Label();
        methodVisitor.visitLabel(label9);
        methodVisitor.visitLineNumber(62, label9);
        methodVisitor.visitVarInsn(ALOAD, 7);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "javax/crypto/Cipher", "doFinal", "([B)[B", false);
        methodVisitor.visitVarInsn(ASTORE, 8);
        Label label10 = new Label();
        methodVisitor.visitLabel(label10);
        methodVisitor.visitLineNumber(64, label10);
        methodVisitor.visitTypeInsn(NEW, "java/lang/String");
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitVarInsn(ALOAD, 8);
        methodVisitor.visitFieldInsn(GETSTATIC, "java/nio/charset/StandardCharsets", "UTF_16", "Ljava/nio/charset/Charset;");
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>", "([BLjava/nio/charset/Charset;)V", false);
        methodVisitor.visitLabel(label1);
        methodVisitor.visitInsn(ARETURN);
        methodVisitor.visitLabel(label2);
        methodVisitor.visitLineNumber(65, label2);
        methodVisitor.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/Throwable"});
        methodVisitor.visitVarInsn(ASTORE, 3);
        Label label11 = new Label();
        methodVisitor.visitLabel(label11);
        methodVisitor.visitLineNumber(66, label11);
        methodVisitor.visitTypeInsn(NEW, "java/lang/IllegalStateException");
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitLdcInsn("Failed to decrypt");
        methodVisitor.visitVarInsn(ALOAD, 3);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalStateException", "<init>", "(Ljava/lang/String;Ljava/lang/Throwable;)V", false);
        methodVisitor.visitInsn(ATHROW);
        Label label12 = new Label();
        methodVisitor.visitLabel(label12);
        methodVisitor.visitMaxs(4, 9);
        methodVisitor.visitEnd();


        // /!\ Absolutely critical line
        skidMethodNode.recomputeCfg();
        EventBus.call(
                new InitMethodTransformEvent(skidMethodNode.getSkidfuscator(), skidMethodNode)
        );
    }
}
